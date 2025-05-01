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

import com.janilla.net.Net;
import com.janilla.util.EntryList;

public abstract class Http {

	public static EntryList<String, String> parseCookieHeader(String string) {
		return Net.parseEntryList(string, ";", "=");
	}

//	public static HttpResponse fetch(InetSocketAddress address, HttpRequest request) {
//		SocketChannel ch;
//		try {
//			ch = SocketChannel.open();
//			ch.configureBlocking(true);
//			ch.connect(address);
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//		SSLContext sc;
//		try {
//			sc = SSLContext.getInstance("TLSv1.3");
//			sc.init(null, null, null);
//		} catch (GeneralSecurityException e) {
//			throw new RuntimeException(e);
//		}
	////		try (var is = Net.class.getResourceAsStream("testkeys")) {
////			sc = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
////		} catch (IOException e) {
////			throw new UncheckedIOException(e);
////		}
//		var se = sc.createSSLEngine(address.getHostName(), address.getPort());
//		se.setUseClientMode(true);
//		var spp = se.getSSLParameters();
//		spp.setApplicationProtocols(new String[] { "h2" });
//		se.setSSLParameters(spp);
//		try (var sch = new SSLByteChannel(ch, se)) {
//			try {
//				sch.write(ByteBuffer.wrap(HttpProtocol.CLIENT_CONNECTION_PREFACE_PREFIX.getBytes()));
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			{
//				var fo = new Frame.Settings(false,
//						List.of(new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 65535),
//								new Setting.Parameter(Setting.Name.HEADER_TABLE_SIZE, 4096),
//								new Setting.Parameter(Setting.Name.MAX_FRAME_SIZE, 16384)));
	////				System.out.println("Http.fetch, fo=" + fo);
//				Http.encode(fo, sch);
//			}
//			var hd = new HeaderDecoder();
//			for (;;) {
//				var fi = Http.decode(sch, hd);
	////				System.out.println("Http.fetch, fi=" + fi);
//				if (fi instanceof Frame.Settings fs && fs.ack())
//					break;
//			}
//			var ffo = new ArrayList<Frame>();
//			ffo.add(new Frame.Settings(true, List.of()));
//			request.setScheme("https");
//			request.setAuthority(address.getHostName());
//			request.setHeaderValue("user-agent", "curl/7.88.1");
//			ffo.add(new Frame.Headers(false, true, request.getBody() == null || request.getBody().length == 0, 1, false,
//					0, 0,
	////					java.util.stream.Stream
////							.concat(java.util.stream.Stream.of(new HeaderField(":method", request.getMethod()),
////									new HeaderField(":path", request.getPath()), new HeaderField(":scheme", "https"),
////									new HeaderField(":authority", address.getHostName()),
////									new HeaderField("user-agent", "curl/7.88.1")),request.getHeaders().stream())
////							.toList()));
//					request.getHeaders()));
//			if (request.getBody() != null && request.getBody().length > 0) {
//				var n = Math.ceilDiv(request.getBody().length, 16384);
//				IntStream.range(0, n).mapToObj(x -> {
//					var bb = new byte[Math.min(16384, request.getBody().length - x * 16384)];
//					System.arraycopy(request.getBody(), x * 16384, bb, 0, bb.length);
//					return new Frame.Data(false, x == n - 1, 1, bb);
//				}).forEach(ffo::add);
//			}
//			for (var fo : ffo) {
	////				System.out.println("Http.fetch, fo=" + fo);
//				Http.encode(fo, sch);
//			}
//			var ff1 = new ArrayList<Frame>();
//			for (;;) {
//				var f1 = Http.decode(sch, hd);
	////				System.out.println("Http.fetch, fi=" + f1);
//				if (f1 instanceof Frame.Headers || f1 instanceof Frame.Data) {
//					ff1.add(f1);
//					var es = f1 instanceof Frame.Headers x ? x.endStream()
//							: f1 instanceof Frame.Data x ? x.endStream() : false;
//					if (es)
//						break;
//				}
//			}
	////			int status = 0;
//			var hf1 = (Frame.Headers) ff1.get(0);
//			var hh = new ArrayList<HeaderField>();
//			for (var f : hf1.fields()) {
	////				switch (f.name()) {
////				case ":status":
////					status = Integer.parseInt(f.value());
////					break;
////				default:
////					hh.add(f);
////				}

//				hh.add(f);
//			}
//			var rs = new HttpResponse();
	////			rs.setStatus(status);
//			rs.setHeaders(hh);
//			if (ff1.size() > 1) {
//				var ii = new int[ff1.size()];
//				for (var i = 1; i < ii.length; i++)
//					ii[i] = ii[i - 1] + ((Frame.Data) ff1.get(i)).data().length;
//				var bb = new byte[ii[ii.length - 1]];
//				for (var i = 1; i < ii.length; i++) {
//					var d = ((Frame.Data) ff1.get(i)).data();
//					System.arraycopy(d, 0, bb, ii[i - 1], d.length);
//				}
//				rs.setBody(bb);
//			}
//			return rs;
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
}
