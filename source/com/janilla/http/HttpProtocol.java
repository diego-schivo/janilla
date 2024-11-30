/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;

import com.janilla.net.Connection;
import com.janilla.net.Protocol;
import com.janilla.net.SSLByteChannel;
import com.janilla.web.HandleException;

public class HttpProtocol implements Protocol {

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
			var ff = c.getStreams().computeIfAbsent(si, x -> new ArrayList<>());
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
						rq.setBody(bb);
					}
//					System.out.println(LocalTime.now() + ", HttpProtocol.handle, c=" + c.getId() + ", si=" + si
//							+ ", rq=" + rq.getMethod() + " " + rq.getPath());
					var rs = new HttpResponse();
					rs.setHeaders(new ArrayList<>(List.of(new HeaderField(":status", null))));
					var ex = createExchange(rq);
					ex.setRequest(rq);
					ex.setResponse(rs);
					@SuppressWarnings("unused")
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
//					System.out.println(LocalTime.now() + ", HttpProtocol.handle, c=" + c.getId() + ", si=" + si
//							+ ", rs=" + rs.getStatus() + ", k=" + k);
					var off = new ArrayList<Frame>(List.of(new Frame.Headers(false, true,
							rs.getBody() == null || rs.getBody().length == 0, si, false, 0, 0, rs.getHeaders())));
					if (rs.getBody() != null && rs.getBody().length > 0) {
						var n = Math.ceilDiv(rs.getBody().length, 16384);
						IntStream.range(0, n).mapToObj(x -> {
							var bb = new byte[Math.min(16384, rs.getBody().length - x * 16384)];
							System.arraycopy(rs.getBody(), x * 16384, bb, 0, bb.length);
							return new Frame.Data(false, x == n - 1, si, bb);
						}).forEach(off::add);
					}
					c.getStreams().remove(si);
					for (var of : off) {
//						System.out.println("HttpProtocol.handle, c=" + c.getId() + ", si=" + si + ", of=" + of);
						synchronized (ch) {
							Http.encode(of, ch);
						}
					}
				});
			}
		}
		return true;
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}
}
