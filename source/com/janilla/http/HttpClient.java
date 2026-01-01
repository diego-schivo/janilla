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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.json.Json;
import com.janilla.net.SecureTransfer;

public class HttpClient {

	private static SSLContext sslContext(String protocol) {
		try {
			var sc = SSLContext.getInstance(protocol);
			sc.init(null, null, null);
			return sc;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected final SSLContext sslContext;

	public HttpClient() {
		this(sslContext("TLSv1.3"));
	}

	public HttpClient(String protocol) {
		this(sslContext(protocol));
	}

	public HttpClient(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public Object getJson(URI uri) {
		return getJson(uri, null);
	}

	public Object getJson(URI uri, String cookie) {
//		IO.println("HttpClient.getJson, uri=" + uri + ", cookie=" + cookie);
		var rq = new HttpRequest();
		rq.setMethod("GET");
		rq.setTarget(
				Stream.of(uri.getPath(), uri.getQuery()).filter(Objects::nonNull).collect(Collectors.joining("?")));
		rq.setScheme(uri.getScheme());
		rq.setAuthority(uri.getAuthority());
		if (cookie != null && !cookie.isEmpty())
			rq.setHeaderValue("cookie", cookie);
		return send(rq, rs -> {
			try (var x = Channels.newInputStream((ReadableByteChannel) rs.getBody())) {
				var s = new String(x.readAllBytes());
//				IO.println("HttpClient.getJson, s=" + s);
				return Json.parse(s);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public <R> R send(HttpRequest request, Function<HttpResponse, R> function) {
		try (var ch = SocketChannel.open()) {
			var a = request.getHeaderValue(":authority");
			var i = a.indexOf(':');
			var h = i != -1 ? a.substring(0, i) : a;
			var p = i != -1 ? Integer.parseInt(a.substring(i + 1)) : 443;
//			IO.println("HttpClient.send, h=" + h + ", p=" + p);
			ch.connect(new InetSocketAddress(h, p));

			var e = sslContext.createSSLEngine();
			e.setUseClientMode(true);
			var pp = e.getSSLParameters();
			pp.setApplicationProtocols(new String[] { "h2" });
			e.setSSLParameters(pp);

			var t = new SecureTransfer(ch, e);
			var he = new HttpEncoder();
			var hd = new HttpDecoder();

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

			t.out().clear();
			t.out().put(he.encodeFrame(new SettingsFrame(false,
					List.of(new SettingParameter(SettingName.MAX_CONCURRENT_STREAMS, 100),
							new SettingParameter(SettingName.INITIAL_WINDOW_SIZE, 10485760),
							new SettingParameter(SettingName.ENABLE_PUSH, 0)))));
			for (t.out().flip(); t.out().hasRemaining();)
				t.write();

			var ff = new ArrayList<Frame>();
			var bb = request.getBody() instanceof ReadableByteChannel x ? Channels.newInputStream(x).readAllBytes()
					: null;
			ff.add(new HeadersFrame(false, true, bb == null || bb.length == 0, 1, false, 0, 0, request.getHeaders()));
			if (bb != null)
				for (var o = 0; o < bb.length;) {
					var l = Math.min(bb.length - o, 16384);
					ff.add(new DataFrame(false, o + l == bb.length, 1, Arrays.copyOfRange(bb, o, o + l)));
					o += l;
				}
			for (var f : ff) {
				IO.println("HttpClient.send, f=" + f);
				t.out().clear();
				t.out().put(he.encodeFrame(f));
				for (t.out().flip(); t.out().hasRemaining();)
					t.write();
			}

			ff.clear();
			for (var es = false; !es;) {
				while (t.in().position() < 3)
					t.read();
				var l = (Short.toUnsignedInt(t.in().getShort(0)) << 8) | Byte.toUnsignedInt(t.in().get(Short.BYTES));
				if (l > 16384)
					throw new RuntimeException();
//				IO.println("HttpClient.send, l=" + l);

				bb = new byte[9 + l];
				while (t.in().position() < bb.length)
					t.read();
				t.in().flip();
				t.in().get(bb);
				t.in().compact();
				var f = hd.decodeFrame(bb);
//				IO.println("HttpClient.send, f=" + f);

				switch (f) {
				case HeadersFrame x:
					ff.add(x);
					if (x.endStream())
						es = true;
					break;
				case DataFrame x:
					ff.add(x);
					if (x.endStream())
						es = true;
					break;
				case SettingsFrame _:
				case PingFrame _:
//				case Frame.Goaway _:
				case WindowUpdateFrame _:
					break;
				default:
					throw new RuntimeException(f.toString());
				}
			}

			R r;
			try (var rs = new HttpResponse()) {
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
//				IO.println("HttpClient.send, rs=" + rs);
				r = function.apply(rs);
			}

			t.out().clear();
			t.out().put(he.encodeFrame(new GoawayFrame(1, 0, new byte[0])));
			for (t.out().flip(); t.out().hasRemaining();)
				t.write();

//			IO.println("HttpClient.send, closeOutbound");
			e.closeOutbound();
			do
				t.write();
			while (!e.isOutboundDone());

			var ci = true;
			do {
//				IO.println("HttpClient.send, ci=" + ci);
				var n = t.read();
//				IO.println("HttpClient.send, n=" + n);
				if (n == -1) {
					ci = false;
					break;
				}
			} while (!e.isInboundDone());
			if (ci) {
//				IO.println("HttpClient.send, closeInbound");
				e.closeInbound();
			}
			return r;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
