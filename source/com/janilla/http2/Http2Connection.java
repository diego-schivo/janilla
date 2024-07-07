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

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.net.Connection;
import com.janilla.net.SSLByteChannel;

public class Http2Connection extends Connection {

	HeaderDecoder headerDecoder = new HeaderDecoder();

	Collection<Stream> streams = new HashSet<>();

	public Http2Connection(int number, SocketChannel channel, SSLByteChannel sslChannel) {
		super(number, channel, sslChannel);
	}

	HeaderDecoder headerDecoder() {
		return headerDecoder;
	}

	Collection<Stream> streams() {
		return streams;
	}

//	@Override
//	public Exchange newExchange() {
//		if (streams().isEmpty()) {
//			var s = new Stream(0, new ArrayList<>());
//			streams().add(s);
//
//			var bb = new byte[CLIENT_CONNECTION_PREFACE_PREFIX.length()];
//			String iccpp;
//			try {
//				iccpp = IO.read(sslChannel(), bb) == bb.length ? new String(bb) : null;
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			System.err.println("iccpp=" + iccpp);
//
//			var isf1 = (SettingsFrame) Frame.decode(sslChannel(), headerDecoder());
//			System.err.println("isf1=" + isf1);
//			s.entries().add(new Stream.Entry(isf1, false));
//
//			var osf1 = new SettingsFrame(false, Map.of(SettingName.INITIAL_WINDOW_SIZE, 65535,
//					SettingName.HEADER_TABLE_SIZE, 4096, SettingName.MAX_FRAME_SIZE, 16384));
//			System.err.println("osf1=" + osf1);
//			Frame.encode(osf1, sslChannel());
//			s.entries().add(new Stream.Entry(osf1, true));
//
//			var osf2 = new SettingsFrame(true, Map.of());
//			System.err.println("osf2=" + osf2);
//			Frame.encode(osf2, sslChannel());
//			s.entries().add(new Stream.Entry(osf2, true));
//		} else {
//			var if1 = Frame.decode(sslChannel(), headerDecoder());
//			System.err.println("if1=" + if1);
//			if (if1 instanceof GoawayFrame x)
//				System.err.println(
//						"x.additionalDebugData()=\n" + Util.toHexString(Util.toIntStream(x.additionalDebugData())));
//			var s = streams().stream().filter(x -> x.identifier() == if1.streamIdentifier()).findFirst()
//					.orElseGet(() -> {
//						var x = new Stream(if1.streamIdentifier(), new ArrayList<>());
//						streams().add(x);
//						return x;
//					});
//			s.entries().add(new Stream.Entry(if1, false));
//			var es = (if1 instanceof HeadersFrame x && x.endStream()) || (if1 instanceof DataFrame y && y.endStream());
//			if (es) {
//				var ihf = (HeadersFrame) s.entries().stream()
//						.filter(x -> !x.output() && x.frame() instanceof HeadersFrame).findFirst()
//						.map(Stream.Entry::frame).get();
//				var idff = s.entries().stream().filter(x -> !x.output() && x.frame() instanceof DataFrame)
//						.map(x -> (DataFrame) x.frame()).toList();
//				String method = null, scheme = null, authority = null, path = null;
//				var hh = new ArrayList<HeaderField>();
//				for (var f : ihf.fields()) {
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
//						hh.add(new HeaderField(f.name(), f.value()));
//					}
//				}
//				var rq = new HttpRequest();
//				rq.setMethod(method);
//				rq.setUri(URI.create(scheme + "://" + authority + path));
//				rq.setHeaders(hh);
//				var p = new int[1];
//				rq.setBody(
//						idff.stream().reduce(new byte[idff.stream().mapToInt(x -> x.data().length).sum()], (x, y) -> {
//							System.arraycopy(y.data(), 0, x, p[0], y.data().length);
//							p[0] += y.data().length;
//							return x;
//						}, (x1, x2) -> x1));
//				var rs = new HttpResponse();
//				return new Http2Exchange(rq, rs, () -> {
//					var ohf = new HeadersFrame(false, true, false, s.identifier(), false, 0, 0, java.util.stream.Stream
//							.concat(java.util.stream.Stream
//									.of(new com.janilla.hpack.HeaderField(":status", String.valueOf(rs.getStatus()))),
//									rs.getHeaders().stream()
//											.map(x -> new com.janilla.hpack.HeaderField(x.name(), x.value())))
//							.toList());
//					System.err.println("ohf=" + ohf);
//					Frame.encode(ohf, sslChannel());
//					s.entries().add(new Stream.Entry(ohf, true));
//					var n = Math.ceilDiv(rs.getBody().length, 16384);
//					var odff = IntStream.range(0, n).mapToObj(x -> {
//						var bb = new byte[Math.min(16384, rs.getBody().length - x * 16384)];
//						System.arraycopy(rs.getBody(), x * 16384, bb, 0, bb.length);
//						return new DataFrame(false, x == n - 1, s.identifier(), bb);
//					}).toList();
//					for (var odf : odff) {
//						System.err.println("odf=" + odf);
//						Frame.encode(odf, sslChannel());
//						s.entries().add(new Stream.Entry(odf, true));
//					}
//				});
//			}
//		}
//		return null;
//	}
}
