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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.net.SecureTransfer;

public class HttpClient {

	protected final SSLContext sslContext;

	public HttpClient(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public Object getJson(String uri) {
		var u = URI.create(uri);
		var rq = new HttpRequest();
		rq.setMethod("GET");
		rq.setTarget(u.getPath());
		rq.setScheme(u.getScheme());
		rq.setAuthority(u.getAuthority());
		return send(rq, rs -> {
			try {
				return Json
						.parse(new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
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
//			System.out.println("HttpClient.send, h=" + h + ", p=" + p);
			ch.connect(new InetSocketAddress(h, p));

			var e = sslContext.createSSLEngine();
			e.setUseClientMode(true);
			var pp = e.getSSLParameters();
			pp.setApplicationProtocols(new String[] { "h2" });
			e.setSSLParameters(pp);

			var t = new SecureTransfer(ch, e);
			var he = new HttpEncoder();
			var hd = new HttpDecoder();

			t.out().put("""
					PRI * HTTP/2.0\r
					\r
					SM\r
					\r
					""".getBytes());
			for (t.out().flip(); t.out().hasRemaining();)
				t.write();

			t.out().clear();
			t.out().put(he.encodeFrame(new Frame.Settings(false,
					List.of(new Setting.Parameter(Setting.Name.MAX_CONCURRENT_STREAMS, 100),
							new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 10485760),
							new Setting.Parameter(Setting.Name.ENABLE_PUSH, 0)))));
			for (t.out().flip(); t.out().hasRemaining();)
				t.write();

			var ff = new ArrayList<Frame>();
			var bb = request.getBody() instanceof ReadableByteChannel x ? Channels.newInputStream(x).readAllBytes()
					: null;
			ff.add(new Frame.Headers(false, true, bb == null || bb.length == 0, 1, false, 0, 0, request.getHeaders()));
			if (bb != null)
				for (var o = 0; o < bb.length;) {
					var l = Math.min(bb.length - o, 16384);
					ff.add(new Frame.Data(false, o + l == bb.length, 1, Arrays.copyOfRange(bb, o, o + l)));
					o += l;
				}
			for (var f : ff) {
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
//				System.out.println("HttpClient.send, l=" + l);

				bb = new byte[9 + l];
				while (t.in().position() < bb.length)
					t.read();
				t.in().flip();
				t.in().get(bb);
				t.in().compact();
				var f = hd.decodeFrame(bb);
//				System.out.println("HttpClient.send, f=" + f);

				switch (f) {
				case Frame.Headers x:
					ff.add(x);
					if (x.endStream())
						es = true;
					break;
				case Frame.Data x:
					ff.add(x);
					if (x.endStream())
						es = true;
					break;
				case Frame.Settings _:
				case Frame.Ping _:
//				case Frame.Goaway _:
				case Frame.WindowUpdate _:
					break;
				default:
					throw new RuntimeException(f.toString());
				}
			}

			R r;
			try (var rs = new HttpResponse()) {
				var hh = new ArrayList<HeaderField>();
				var b = ByteBuffer.allocate(ff.stream().filter(x -> x instanceof Frame.Data)
						.mapToInt(x -> ((Frame.Data) x).data().length).sum());
				for (var f : ff)
					if (f instanceof Frame.Headers x)
						hh.addAll(x.fields());
					else
						b.put(((Frame.Data) f).data());
				rs.setHeaders(hh);
				rs.setBody(IO.toReadableByteChannel(b.flip()));
//				System.out.println("HttpClient.send, rs=" + rs);
				r = function.apply(rs);
			}

			t.out().clear();
			t.out().put(he.encodeFrame(new Frame.Goaway(1, 0, new byte[0])));
			for (t.out().flip(); t.out().hasRemaining();)
				t.write();

//			System.out.println("HttpClient.send, closeOutbound");
			e.closeOutbound();
			do
				t.write();
			while (!e.isOutboundDone());

			var ci = true;
			do {
//				System.out.println("HttpClient.send, ci=" + ci);
				var n = t.read();
//				System.out.println("HttpClient.send, n=" + n);
				if (n == -1) {
					ci = false;
					break;
				}
			} while (!e.isInboundDone());
			if (ci) {
//				System.out.println("HttpClient.send, closeInbound");
				e.closeInbound();
			}
			return r;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
