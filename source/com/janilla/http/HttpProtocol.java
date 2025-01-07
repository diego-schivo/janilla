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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.Connection;
import com.janilla.net.Protocol;
import com.janilla.net.SSLByteChannel;
import com.janilla.web.HandleException;

public class HttpProtocol implements Protocol {

	public static final ScopedValue<HttpExchange> HTTP_EXCHANGE = ScopedValue.newInstance();

	protected SSLContext sslContext;

	protected HttpHandler handler;

	protected boolean useClientMode;

	int connectionNumber;

	int streamIdentifier;

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void setHandler(HttpHandler handler) {
		this.handler = handler;
	}

	public void setUseClientMode(boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	@Override
	public Connection buildConnection(SocketChannel channel) {
		var se = sslContext.createSSLEngine();
		se.setUseClientMode(useClientMode);
		var pp = se.getSSLParameters();
		pp.setApplicationProtocols(new String[] { "h2" });
		se.setSSLParameters(pp);
		var hc = new HttpConnection();
		hc.setId(++connectionNumber);
		hc.setChannel(channel);
		hc.setSslByteChannel(new SSLByteChannel(channel, se));
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
		var ch = c.getSslByteChannel();
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
			ff.add(f);
			var es = f instanceof Frame.Headers x ? x.endStream() : f instanceof Frame.Data x ? x.endStream() : false;
			if (es) {
				Thread.startVirtualThread(() -> {
					var rq = new HttpRequest();
					rq.setHeaders(((Frame.Headers) ff.get(0)).fields());
					if (ff.size() > 1) {
						var l = ff.stream().skip(1).mapToInt(x -> ((Frame.Data) x).data().length).sum();
						var bb = new byte[l];
						var i = ff.iterator();
						i.next();
						for (var p = 0; p < bb.length;) {
							var d = ((Frame.Data) i.next()).data();
							System.arraycopy(d, 0, bb, p, d.length);
							p += d.length;
						}
						rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
					}
//					System.out.println(LocalTime.now() + ", HttpProtocol.handle, c=" + c.getId() + ", si=" + si
//							+ ", rq=" + rq.getMethod() + " " + rq.getPath());
					var rs = new HttpResponse();
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
				});
			}
		}
		return true;
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}
}
