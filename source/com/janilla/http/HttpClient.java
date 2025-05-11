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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.SecureTransfer;

public class HttpClient {

	protected final SSLContext sslContext;

	public HttpClient(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void send(HttpRequest request, Consumer<HttpResponse> consumer) throws IOException {
		try (var sc = SocketChannel.open()) {
			var a = request.getHeaderValue(":authority");
			var i = a.indexOf(':');
			var hn = i != -1 ? a.substring(0, i) : a;
			var p = i != -1 ? Integer.parseInt(a.substring(i + 1)) : 443;
			sc.connect(new InetSocketAddress(hn, p));

			var se = sslContext.createSSLEngine();
			se.setUseClientMode(true);
			var spp = se.getSSLParameters();
			spp.setApplicationProtocols(new String[] { "h2" });
			se.setSSLParameters(spp);

			var st = new SecureTransfer(sc, se);
			var he = new HttpEncoder();
			var hd = new HttpDecoder();

			st.out().put("""
					PRI * HTTP/2.0\r
					\r
					SM\r
					\r
					""".getBytes());
			for (st.out().flip(); st.out().hasRemaining();)
				st.write();

			st.out().clear();
			st.out().put(he.encodeFrame(new Frame.Settings(false,
					List.of(new Setting.Parameter(Setting.Name.MAX_CONCURRENT_STREAMS, 100),
							new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 10485760),
							new Setting.Parameter(Setting.Name.ENABLE_PUSH, 0)))));
			for (st.out().flip(); st.out().hasRemaining();)
				st.write();

			var ff = new ArrayList<Frame>();
			var bb = Channels.newInputStream((ReadableByteChannel) request.getBody()).readAllBytes();
			ff.add(new Frame.Headers(false, true, bb.length == 0, 1, false, 0, 0, request.getHeaders()));
			for (var o = 0; o < bb.length;) {
				var l = Math.min(bb.length - o, 16384);
				ff.add(new Frame.Data(false, o + l == bb.length, 1, Arrays.copyOfRange(bb, o, o + l)));
				o += l;
			}
			for (var f : ff) {
				st.out().clear();
				st.out().put(he.encodeFrame(f));
				for (st.out().flip(); st.out().hasRemaining();)
					st.write();
			}

			ff.clear();
			for (var es = false; !es;) {
				while (st.in().position() < 3)
					st.read();
				var pl = (Short.toUnsignedInt(st.in().getShort(0)) << 8) | Byte.toUnsignedInt(st.in().get(Short.BYTES));
				if (pl > 16384)
					throw new RuntimeException();

				bb = new byte[9 + pl];
				while (st.in().position() < bb.length)
					st.read();
				st.in().flip();
				st.in().get(bb);
				st.in().compact();
				var f = hd.decodeFrame(bb);
//				System.out.println("C: f=" + f);

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

			try (var rs = new HttpResponse()) {
				var hff = new ArrayList<HeaderField>();
				var dbb = ByteBuffer.allocate(ff.stream().filter(x -> x instanceof Frame.Data)
						.mapToInt(x -> ((Frame.Data) x).data().length).sum());
				for (var f : ff)
					if (f instanceof Frame.Headers x)
						hff.addAll(x.fields());
					else
						dbb.put(((Frame.Data) f).data());
				rs.setHeaders(hff);
				rs.setBody(IO.toReadableByteChannel(dbb.flip()));
//				System.out.println("C: rs=" + rs);
				consumer.accept(rs);
			}

			st.out().clear();
			st.out().put(he.encodeFrame(new Frame.Goaway(1, 0, new byte[0])));
			for (st.out().flip(); st.out().hasRemaining();)
				st.write();

			System.out.println("C: closeOutbound");
			se.closeOutbound();
			do
				st.write();
			while (!se.isOutboundDone());

			var ci = true;
			do
				if (st.read() == -1) {
					ci = false;
					break;
				}
			while (!se.isInboundDone());
			if (ci) {
				System.out.println("C: closeInbound");
				se.closeInbound();
			}
		}
	}
}
