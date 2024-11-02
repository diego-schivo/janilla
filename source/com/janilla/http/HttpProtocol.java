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
	public void handle(Connection connection) {
		var c = (HttpConnection) connection;
//		System.out.println("HttpProtocol.handle, c=" + c.getId());
		var bc = c.getSslByteChannel();
		if (!c.isPrefaceReceived()) {
			var bb = ByteBuffer.allocate(24);
			try {
				bc.read(bb);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", bb=" + new String(bb.array()));
			c.setPrefaceReceived(true);
		}
		var f1 = Http.decode(bc, c.getHeaderDecoder());
		if (f1 == null)
			return;
//		System.out.println("HttpProtocol.handle, c=" + c.getId() + ", f1=" + f1);
		var ff2 = new ArrayList<Frame>();
		if (!c.isPrefaceSent()) {
			var f2 = new Frame.Settings(false,
					List.of(new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 65535),
							new Setting.Parameter(Setting.Name.HEADER_TABLE_SIZE, 4096),
							new Setting.Parameter(Setting.Name.MAX_FRAME_SIZE, 16384)));
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", f2=" + f2);
			Http.encode(f2, bc);
			f2 = new Frame.Settings(true, List.of());
			ff2.add(f2);
			c.setPrefaceSent(true);
		}
		if (f1 instanceof Frame.Headers || f1 instanceof Frame.Data) {
			var si = Integer.valueOf(f1.streamIdentifier());
			var ff1 = c.getStreams().computeIfAbsent(si, x -> new ArrayList<>());
			ff1.add(f1);
			var es = f1 instanceof Frame.Headers x ? x.endStream() : f1 instanceof Frame.Data x ? x.endStream() : false;
			if (es) {
//				String method = null, scheme = null, authority = null, path = null;
				var hf1 = (Frame.Headers) ff1.get(0);
				var hh = new ArrayList<HeaderField>();
				for (var f : hf1.fields()) {
//					switch (f.name()) {
//					case ":method":
//						method = f.value();
//						break;
//					case ":scheme":
//						scheme = f.value();
//						break;
//					case ":authority":
//						authority = f.value();
//						break;
//					case ":path":
//						path = f.value();
//						break;
//					default:
//						hh.add(f);
//					}
					hh.add(f);
				}
				var rq = new HttpRequest();
//				rq.setMethod(method);
//				rq.setScheme(scheme);
//				rq.setAuthority(authority);
//				rq.setTarget(path);
				rq.setHeaders(hh);
				if (ff1.size() > 1) {
					var ii = new int[ff1.size()];
					for (var i = 1; i < ii.length; i++)
						ii[i] = ii[i - 1] + ((Frame.Data) ff1.get(i)).data().length;
					var bb = new byte[ii[ii.length - 1]];
					for (var i = 1; i < ii.length; i++) {
						var d = ((Frame.Data) ff1.get(i)).data();
						System.arraycopy(d, 0, bb, ii[i - 1], d.length);
					}
					rq.setBody(bb);
				}
//				System.out
//						.println("HttpProtocol.handle, c=" + c.getId() + ", rq=" + rq.getMethod() + " " + rq.getPath());
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
//				System.out.println("HttpProtocol.handle, c=" + c.getId() + ", rs=" + rs.getStatus() + ", k=" + k);
				var hf2 = new Frame.Headers(false, true, rs.getBody() == null || rs.getBody().length == 0,
						hf1.streamIdentifier(), false, 0, 0,
//						java.util.stream.Stream.concat(
//								java.util.stream.Stream.of(new HeaderField(":status", String.valueOf(rs.getStatus()))),
//								rs.getHeaders() != null ? rs.getHeaders().stream() : java.util.stream.Stream.empty())
//								.toList());
						rs.getHeaders());
				ff2.add(hf2);
				if (rs.getBody() != null && rs.getBody().length > 0) {
					var n = Math.ceilDiv(rs.getBody().length, 16384);
					IntStream.range(0, n).mapToObj(x -> {
						var bb = new byte[Math.min(16384, rs.getBody().length - x * 16384)];
						System.arraycopy(rs.getBody(), x * 16384, bb, 0, bb.length);
						return new Frame.Data(false, x == n - 1, hf1.streamIdentifier(), bb);
					}).forEach(ff2::add);
				}
				c.getStreams().remove(si);
			}
		}
		for (var f2 : ff2) {
//			System.out.println("HttpProtocol.handle, c=" + c.getId() + ", f2=" + f2);
			Http.encode(f2, bc);
		}
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}
}
