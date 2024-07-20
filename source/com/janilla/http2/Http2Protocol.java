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
package com.janilla.http2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpProtocol;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.media.HeaderField;
import com.janilla.net.Connection;
import com.janilla.net.SSLByteChannel;

public class Http2Protocol extends HttpProtocol {

	int connectionNumber;

	@Override
	public Connection buildConnection(SocketChannel channel) {
		var se = sslContext.createSSLEngine();
		se.setUseClientMode(false);
		var pp = se.getSSLParameters();
		pp.setApplicationProtocols(new String[] { "h2" });
		se.setSSLParameters(pp);
		return new Http2Connection(++connectionNumber, channel, new SSLByteChannel(channel, se));
	}

	static String CLIENT_CONNECTION_PREFACE_PREFIX = """
			PRI * HTTP/2.0\r
			\r
			SM\r
			\r
			""";

	@Override
	public void handle(Connection connection) {
		var c = (Http2Connection) connection;
		var ac = c.applicationChannel();
		if (!c.p1) {
			var bb = ByteBuffer.allocate(24);
			try {
				ac.read(bb);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
//			System.out.println(c.number() + " " + new String(cc));
			c.p1 = true;
		}
		if (c.p1) {
			var ff1 = new ArrayList<Frame>();
			{
				var f1 = Frame.decode(ac, c.headerDecoder());
//				System.out.println(c.number() + " f1=" + f1);
				ff1.add(f1);
			}
			var ff2 = new ArrayList<Frame>();
			if (!c.p2) {
				var f2 = new Frame.Settings(false,
						List.of(new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 65535),
								new Setting.Parameter(Setting.Name.HEADER_TABLE_SIZE, 4096),
								new Setting.Parameter(Setting.Name.MAX_FRAME_SIZE, 16384)));
				Frame.encode(f2, ac);
				f2 = new Frame.Settings(true, List.of());
				ff2.add(f2);
				c.p2 = true;
			}
			for (var f1 : ff1)
				if (f1 instanceof Frame.Headers hf1) {
					String method = null, scheme = null, authority = null, path = null;
					var hh = new ArrayList<HeaderField>();
					for (var y : hf1.fields()) {
						switch (y.name()) {
						case ":method":
							method = y.value();
							break;
						case ":scheme":
							scheme = y.value();
							break;
						case ":authority":
							authority = y.value();
							break;
						case ":path":
							path = y.value();
							break;
						default:
							hh.add(y);
						}
					}
					var rq = new HttpRequest();
					rq.setMethod(method);
					rq.setUri(URI.create(scheme + "://" + authority + path));
					rq.setHeaders(hh);
					System.out.println(c.number() + " rq=" + rq.getMethod() + " " + rq.getUri());
					var rs = new HttpResponse();
					var ex = createExchange(rq);
					ex.setRequest(rq);
					ex.setResponse(rs);
					var k = true;
					Exception e;
					try {
						k = handler.handle(ex);
						e = null;
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
					System.out.println(c.number() + " rs=" + rs.getStatus());
					var hf2 = new Frame.Headers(false, true, rs.getBody() == null || rs.getBody().length == 0,
							hf1.streamIdentifier(), false, 0, 0,
							java.util.stream.Stream.concat(
									java.util.stream.Stream
											.of(new HeaderField(":status", String.valueOf(rs.getStatus()))),
									rs.getHeaders() != null ? rs.getHeaders().stream()
											: java.util.stream.Stream.empty())
									.toList());
					ff2.add(hf2);
					if (rs.getBody() != null && rs.getBody().length > 0) {
						var n = Math.ceilDiv(rs.getBody().length, 16384);
						IntStream.range(0, n).mapToObj(x -> {
							var bb = new byte[Math.min(16384, rs.getBody().length - x * 16384)];
							System.arraycopy(rs.getBody(), x * 16384, bb, 0, bb.length);
							return new Frame.Data(false, x == n - 1, hf1.streamIdentifier(), bb);
						}).forEach(ff2::add);
					}
				}
			for (var f2 : ff2) {
//				System.out.println(c.number() + " f2=" + f2);
				Frame.encode(f2, ac);
			}
		}
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}
}
