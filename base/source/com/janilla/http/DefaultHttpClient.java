/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import com.janilla.net.SecureTransfer;
import com.janilla.net.SimpleTransfer;
import com.janilla.net.Transfer;

public class DefaultHttpClient implements HttpClient {

	public static SSLContext sslContext(String protocol) {
		try {
			var c = SSLContext.getInstance(protocol);
			c.init(null, null, null);
			return c;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected final SSLContext sslContext;

//	public DefaultHttpClient() {
//		this("TLSv1.3");
//	}
//
//	public DefaultHttpClient(String protocol) {
//		this(sslContext(protocol));
//	}

	public DefaultHttpClient() {
		this(null);
	}

	public DefaultHttpClient(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	@Override
	public <R> R send(HttpRequest request, Function<HttpResponse, R> function) {
//		IO.println("HttpClient.send, " + request.getMethod() + " " + request.getUri());
		try (var ch = SocketChannel.open()) {
			{
				var a = request.getHeaderValue(":authority");
				if (a == null)
					a = request.getHeaderValue("Host");
				var i = a.indexOf(':');
				var h = i != -1 ? a.substring(0, i) : a;
				var p = i != -1 ? Integer.parseInt(a.substring(i + 1)) : 443;
//				IO.println("HttpClient.send, h=" + h + ", p=" + p);
				ch.connect(new InetSocketAddress(h, p));
			}

			Transfer t;
			if (sslContext != null) {
				var en = sslContext.createSSLEngine();
				en.setUseClientMode(true);
				var pp = en.getSSLParameters();
				pp.setApplicationProtocols(new String[] { "h2" });
				en.setSSLParameters(pp);
				t = new SecureTransfer(ch, en);
			} else
				t = new SimpleTransfer(ch);

			FrameTransfer t2;
			var rs = new HttpResponse();
			if (t instanceof SecureTransfer) {
				class A {
					private static final byte[] CONNECTION_PREFACE_PREFIX = """
							PRI * HTTP/2.0\r
							\r
							SM\r
							\r
							""".getBytes();
				}
				t.out().put(A.CONNECTION_PREFACE_PREFIX);
				for (t.out().flip(); t.out().hasRemaining();)
					t.write();

				t2 = new FrameTransfer(t);
				t2.writeFrame(new SettingsFrame(false,
						List.of(new SettingParameter(SettingName.MAX_CONCURRENT_STREAMS, 100),
								new SettingParameter(SettingName.INITIAL_WINDOW_SIZE, 10485760),
								new SettingParameter(SettingName.ENABLE_PUSH, 0))));

				var ff = new ArrayList<Frame>();
				var bb = request.getBody() instanceof ReadableByteChannel x ? Channels.newInputStream(x).readAllBytes()
						: null;
//			IO.println("bb=" + new String(bb));
				ff.add(new HeadersFrame(false, true, bb == null || bb.length == 0, 1, false, 0, 0,
						request.getHeaders()));
				if (bb != null)
					for (var o = 0; o < bb.length;) {
						var l = Math.min(bb.length - o, 16384);
						ff.add(new DataFrame(false, o + l == bb.length, 1, Arrays.copyOfRange(bb, o, o + l)));
						o += l;
					}
				for (var f : ff)
					t2.writeFrame(f);

				t.in().flip();
				ff.clear();
				for (var es = false; !es;) {
					var f = t2.readFrame();
					if (f == null)
						break;

					switch (f) {
					case HeadersFrame x:
						ff.add(x);
						if (x.endStream())
							es = true;
						break;

					case DataFrame x:
						ff.add(x);

						if (x.data().length != 0)
							for (var id : new int[] { x.streamIdentifier(), 0 })
								t2.writeFrame(new WindowUpdateFrame(id, 9 + x.data().length));

						if (x.endStream())
							es = true;
						break;

					case SettingsFrame x:
						if (!x.ack())
							t2.writeFrame(new SettingsFrame(true, List.of()));
						break;

					case PingFrame _:
//				case Frame.Goaway _:
					case WindowUpdateFrame _:
						break;
					default:
						throw new RuntimeException(f.toString());
					}
				}

				var hh = new ArrayList<HeaderField>();
				var b = ByteBuffer.allocate(ff.stream().filter(x -> x instanceof DataFrame)
						.mapToInt(x -> ((DataFrame) x).data().length).sum());
				for (var f : ff)
					if (f instanceof HeadersFrame x)
						hh.addAll(x.fields());
					else
						b.put(((DataFrame) f).data());
				rs.setHeaders(hh);
				rs.setBody(Channels.newChannel(new ByteArrayInputStream(b.array())));
			} else {
				t2 = null;

				var ll = new ArrayList<String>();
				ll.add(request.getHeaderValue(":method") + " " + request.getHeaderValue(":path") + " HTTP/1.1");
				request.setHeaderValue("Host", request.getHeaderValue(":authority"));
				for (var h : request.getHeaders())
					if (!h.name().startsWith(":"))
						ll.add(h.toLine());
				ll.add("");
				for (var l : ll) {
					t.out().put((l + "\r\n").getBytes());
					t.out().flip();
					do
						t.write();
					while (t.out().hasRemaining());
					t.out().clear();
				}
				if (request.getBody() instanceof ReadableByteChannel x) {
					var bb = Channels.newInputStream(x).readAllBytes();
					for (var i = 0; i < bb.length;) {
						var n = Math.min(t.out().remaining(), bb.length - i);
						t.out().put(bb, i, n);
						t.out().flip();
						do
							t.write();
						while (t.out().hasRemaining());
						t.out().clear();
						i += n;
					}
				}

				ll.clear();
				for (;;) {
					int i = 0, b1 = -1, b2;
					for (;; i++, b1 = b2) {
						if (i == t.in().position()) {
							var n = t.read();
//							IO.println("n=" + n);
							if (n == -1)
								throw new RuntimeException();
						}
						b2 = t.in().get(i);
						if (b1 == '\r' && b2 == '\n')
							break;
					}
//					IO.println("i=" + i);
					t.in().flip();
					var bb = new byte[i + 1];
					t.in().get(bb);
					t.in().compact();
					var l = new String(bb, 0, i - 1);
//					IO.println("s=" + s);
					if (!l.isEmpty())
						ll.add(l);
					else
						break;
				}

				{
					var i = 0;
					for (var l : ll) {
						if (i == 0) {
							var ss = l.split(" ", 3);
							rs.setHeaderValue(":status", ss[1].trim());
						} else
							rs.setHeader(HeaderField.fromLine(l));
						i++;
					}
				}

				var cl = rs.getHeaderValue("Content-Length");
				if (cl != null) {
					var bb = new byte[Integer.parseInt(cl)];
					for (var i = 0; i < bb.length;) {
						if (t.in().position() == 0)
							t.read();
						t.in().flip();
						var n = Math.min(t.in().remaining(), bb.length - i);
						t.in().get(bb, i, n);
						t.in().compact();
						i += n;
//						IO.println("HttpServer.handleConnection1, i=" + i);
					}
					rs.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
				}
			}
//			IO.println("HttpClient.send, rs=" + rs);

			R r;
			try (var rs2 = rs) {
				r = function.apply(rs2);
			}

			if (t instanceof SecureTransfer st) {
				t2.writeFrame(new GoawayFrame(1, 0, new byte[0]));

//				IO.println("HttpClient.send, closeOutbound");
				st.engine().closeOutbound();
				do
					st.write();
				while (!st.engine().isOutboundDone());
			}

//			var ci = true;
//			do {
//				IO.println("HttpClient.send, ci=" + ci);
//				var n = t.read();
//				IO.println("HttpClient.send, n=" + n);
//				if (n == -1) {
//					ci = false;
//					break;
//				}
//			} while (!e.isInboundDone());
//			if (ci) {
//				IO.println("HttpClient.send, closeInbound");
//				e.closeInbound();
//			}
			return r;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
