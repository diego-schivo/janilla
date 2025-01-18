/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.Connection;
import com.janilla.net.Protocol;
import com.janilla.net.SslByteChannel;
import com.janilla.web.HandleException;

public class HttpProtocol implements Protocol {

	public static final ScopedValue<HttpExchange> HTTP_EXCHANGE = ScopedValue.newInstance();

	protected final HttpHandler handler;

	protected final SSLContext sslContext;

	protected final boolean useClientMode;

	protected int connectionNumber;

	protected int streamIdentifier;

	public HttpProtocol(HttpHandler handler, SSLContext sslContext, boolean useClientMode) {
		super();
		this.handler = handler;
		this.sslContext = sslContext;
		this.useClientMode = useClientMode;
	}

	@Override
	public Connection buildConnection(ByteChannel channel) {
		var se = sslContext.createSSLEngine();
		se.setUseClientMode(useClientMode);
		var pp = se.getSSLParameters();
		pp.setApplicationProtocols(new String[] { "h2" });
		se.setSSLParameters(pp);
		var hc = new HttpConnection();
		hc.setId(++connectionNumber);
		hc.setChannel(new SslByteChannel(channel, se));
		return hc;
	}

	public static String CLIENT_CONNECTION_PREFACE_PREFIX = """
			PRI * HTTP/2.0\r
			\r
			SM\r
			\r
			""";

	@Override
	public boolean handle(Connection connection) {
		var c = (HttpConnection) connection;
//		System.out.println("HttpProtocol.handle, c=" + c.getId());
		var ch = (SslByteChannel) c.getChannel();
		if (!c.isPrefaceReceived()) {
			var b = ByteBuffer.allocate(24);
			try {
				if (ch.read(b) < 24)
					return false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", b=" + new String(b.array()));
			c.setPrefaceReceived(true);
		}
		var f = Http.decode(ch, c.getHeaderDecoder());
		if (f == null)
			return false;
//		System.out.println("HttpProtocol.handle, c=" + c.getId() + ", f=" + f);
		if (!c.isPrefaceSent()) {
			var of = new Frame.Settings(false,
					List.of(new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 65535),
							new Setting.Parameter(Setting.Name.HEADER_TABLE_SIZE, 4096),
							new Setting.Parameter(Setting.Name.MAX_FRAME_SIZE, 16384)));
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", of=" + of);
			Http.encode(of, ch);
			of = new Frame.Settings(true, List.of());
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", of=" + of);
			Http.encode(of, ch);
			c.setPrefaceSent(true);
		}
		if (f instanceof Frame.Headers || f instanceof Frame.Data) {
			var si = Integer.valueOf(f.streamIdentifier());
			var ff = c.getStreams().computeIfAbsent(si, _ -> new ArrayList<>());
			var ffl = new ReentrantLock();
			if (f instanceof Frame.Headers hf && hf.endHeaders())
				Thread.startVirtualThread(() -> {
					try (var rq = new HttpRequest(); var rs = new HttpResponse()) {
						rq.setHeaders(hf.fields());
						rq.setBody(new ReadableByteChannel() {

							private ByteBuffer buffer = ByteBuffer.allocate(1024);

							private boolean ended;

							private boolean closed;

							@Override
							public boolean isOpen() {
								return !closed;
							}

							@Override
							public void close() throws IOException {
								if (closed)
									return;
								closed = true;
							}

							@Override
							public int read(ByteBuffer dst) throws IOException {
								if (closed)
									throw new IOException("closed");
								Frame[] ff2;
								ffl.lock();
								try {
									ff2 = ff.toArray(Frame[]::new);
									ff.clear();
								} finally {
									ffl.unlock();
								}
								var l = Arrays.stream(ff2).mapToInt(x -> ((Frame.Data) x).data().length).sum();
								if (l > 0) {
									var bb = ByteBuffer.allocate(l);
									for (var x : ff2) {
										var d = (Frame.Data) x;
										bb.put(d.data());
										if (d.endStream())
											ended = true;
									}
									if (buffer == null)
										buffer = bb;
									else {
										bb.flip();
										buffer = IO.transferAllRemaining(bb, buffer);
									}
								}
								if (buffer.position() == 0)
									return ended ? -1 : 0;
								buffer.flip();
								try {
									return IO.transferSomeRemaining(buffer, dst);
								} finally {
									buffer.compact();
								}
							}
						});
//					System.out.println(LocalTime.now() + ", HttpProtocol.handle, c=" + c.getId() + ", si=" + si
//							+ ", rq=" + rq.getMethod() + " " + rq.getPath());
						rs.setHeaders(new ArrayList<>(List.of(new HeaderField(":status", null))));
						rs.setBody(new HttpWritableByteChannel() {

							private boolean headersWritten;

							private boolean closed;

							@Override
							public boolean isOpen() {
								return !closed;
							}

							@Override
							public void close() throws IOException {
								if (closed)
									return;
								if (!headersWritten) {
									writeHeaders(true);
									headersWritten = true;
								}
								closed = true;
							}

							@Override
							public int write(ByteBuffer src) throws IOException {
								return write(src, false);
							}

							@Override
							public int write(ByteBuffer src, boolean endStream) throws IOException {
								if (closed)
									throw new IOException("closed");
								var n = src.remaining();
								if (n == 0)
									throw new IllegalArgumentException("src");
								var cd = Math.ceilDiv(n, 16384);
								if (!headersWritten) {
									writeHeaders(false);
									headersWritten = true;
								}
								IntStream.range(0, cd).mapToObj(x -> {
									var bb = new byte[Math.min(16384, n - x * 16384)];
									IO.transferSomeRemaining(src, ByteBuffer.wrap(bb));
									return new Frame.Data(false, endStream && x == cd - 1, si, bb);
								}).forEach(x -> {
									// System.out.println("HttpProtocol.handle, c=" + c.getId() + ", si=" + si + ",
									// of=" + x);
									ch.outboundLock().lock();
									try {
										Http.encode(x, ch);
									} finally {
										ch.outboundLock().unlock();
									}
								});
								return n;
							}

							private void writeHeaders(boolean endStream) {
								if (headersWritten)
									return;
								var of = new Frame.Headers(false, true, endStream, si, false, 0, 0, rs.getHeaders());
//							System.out.println("HttpProtocol.handle, c=" + c.getId() + ", si=" + si + ", of=" + x);
								ch.outboundLock().lock();
								try {
									Http.encode(of, ch);
									headersWritten = true;
								} finally {
									ch.outboundLock().unlock();
								}
							}
						});
						var ex = createExchange(rq);
						ex.setRequest(rq);
						ex.setResponse(rs);
						ScopedValue.callWhere(HTTP_EXCHANGE, ex, () -> {
							var k = true;
							Exception e;
							try {
								k = handler.handle(ex);
								e = null;
							} catch (HandleException x) {
								e = x.getCause();
							} catch (UncheckedIOException x) {
								e = x.getCause();
							} catch (Exception x) {
								e = x;
							}
							if (e != null)
								try {
									e.printStackTrace();
									ex.setException(e);
									k = handler.handle(ex);
								} catch (Exception x) {
									k = false;
								}
							return k;
						});
//					System.out.println(LocalTime.now() + ", HttpProtocol.handle, c=" + c.getId() + ", si=" + si
//							+ ", rs=" + rs.getStatus() + ", k=" + k);
						c.getStreams().remove(si);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			else {
				ffl.lock();
				try {
					ff.add((Frame.Data) f);
				} finally {
					ffl.unlock();
				}
			}
		}
		return true;
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}
}
