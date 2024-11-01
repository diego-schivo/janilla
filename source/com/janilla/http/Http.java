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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.net.SSLByteChannel;
import com.janilla.util.EntryList;

public abstract class Http {

	public static EntryList<String, String> parseCookieHeader(String string) {
		return Net.parseEntryList(string, ";", "=");
	}

	public static HttpResponse fetch(InetSocketAddress address, HttpRequest request) {
		SocketChannel ch;
		try {
			ch = SocketChannel.open();
			ch.configureBlocking(true);
			ch.connect(address);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("TLSv1.3");
			sc.init(null, null, null);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
//		try (var is = Net.class.getResourceAsStream("testkeys")) {
//			sc = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
		var se = sc.createSSLEngine(address.getHostName(), address.getPort());
		se.setUseClientMode(true);
		var spp = se.getSSLParameters();
		spp.setApplicationProtocols(new String[] { "h2" });
		se.setSSLParameters(spp);
		try (var sch = new SSLByteChannel(ch, se)) {
			try {
				sch.write(ByteBuffer.wrap(HttpProtocol.CLIENT_CONNECTION_PREFACE_PREFIX.getBytes()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			{
				var fo = new Frame.Settings(false,
						List.of(new Setting.Parameter(Setting.Name.INITIAL_WINDOW_SIZE, 65535),
								new Setting.Parameter(Setting.Name.HEADER_TABLE_SIZE, 4096),
								new Setting.Parameter(Setting.Name.MAX_FRAME_SIZE, 16384)));
//				System.out.println("Http.fetch, fo=" + fo);
				Http.encode(fo, sch);
			}
			var hd = new HeaderDecoder();
			for (;;) {
				var fi = Http.decode(sch, hd);
//				System.out.println("Http.fetch, fi=" + fi);
				if (fi instanceof Frame.Settings fs && fs.ack())
					break;
			}
			var ffo = new ArrayList<Frame>();
			ffo.add(new Frame.Settings(true, List.of()));
			ffo.add(new Frame.Headers(false, true, request.getBody() == null || request.getBody().length == 0, 1, false,
					0, 0,
					java.util.stream.Stream.concat(
							java.util.stream.Stream.of(new HeaderField(":method", request.getMethod()),
									new HeaderField(":path", request.getPath()), new HeaderField(":scheme", "https"),
									new HeaderField(":authority", address.getHostName()),
									new HeaderField("user-agent", "curl/7.88.1")),
							request.getHeaders() != null ? request.getHeaders().stream()
									: java.util.stream.Stream.empty())
							.toList()));
			if (request.getBody() != null && request.getBody().length > 0) {
				var n = Math.ceilDiv(request.getBody().length, 16384);
				IntStream.range(0, n).mapToObj(x -> {
					var bb = new byte[Math.min(16384, request.getBody().length - x * 16384)];
					System.arraycopy(request.getBody(), x * 16384, bb, 0, bb.length);
					return new Frame.Data(false, x == n - 1, 1, bb);
				}).forEach(ffo::add);
			}
			for (var fo : ffo) {
//				System.out.println("Http.fetch, fo=" + fo);
				Http.encode(fo, sch);
			}
			var ff1 = new ArrayList<Frame>();
			for (;;) {
				var f1 = Http.decode(sch, hd);
//				System.out.println("Http.fetch, fi=" + f1);
				if (f1 instanceof Frame.Headers || f1 instanceof Frame.Data) {
					ff1.add(f1);
					var es = f1 instanceof Frame.Headers x ? x.endStream()
							: f1 instanceof Frame.Data x ? x.endStream() : false;
					if (es)
						break;
				}
			}
			int status = 0;
			var hf1 = (Frame.Headers) ff1.get(0);
			var hh = new ArrayList<HeaderField>();
			for (var f : hf1.fields()) {
				switch (f.name()) {
				case ":status":
					status = Integer.parseInt(f.value());
					break;
				default:
					hh.add(f);
				}
			}
			var rs = new HttpResponse();
			rs.setStatus(status);
			rs.setHeaders(hh);
			if (ff1.size() > 1) {
				var ii = new int[ff1.size()];
				for (var i = 1; i < ii.length; i++)
					ii[i] = ii[i - 1] + ((Frame.Data) ff1.get(i)).data().length;
				var bb = new byte[ii[ii.length - 1]];
				for (var i = 1; i < ii.length; i++) {
					var d = ((Frame.Data) ff1.get(i)).data();
					System.arraycopy(d, 0, bb, ii[i - 1], d.length);
				}
				rs.setBody(bb);
			}
			return rs;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void encode(Frame frame, WritableByteChannel channel) {
		try {
			int pl;
			var bb = ByteBuffer.allocate(9);
			switch (frame) {
			case Frame.Data x:
				pl = x.data().length;
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Frame.Name.DATA.type());
				bb.put((byte) ((x.padded() ? 0x08 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
				bb.putInt(x.streamIdentifier());
				bb.flip();
				channel.write(bb);
				channel.write(ByteBuffer.wrap(x.data()));
				break;
			case Frame.Goaway x:
				throw new RuntimeException();
			case Frame.Headers x:
				var he = new HeaderEncoder();
				var ii = IntStream.builder();
				pl = 0;
				for (var f : x.fields()) {
					switch (f.name()) {
					case ":method", ":scheme", ":status", "content-type":
						pl += he.encode(f, ii);
						break;
					case ":path", "content-length":
//						pl += he.encode(f, ii, false, HeaderField.Representation.NEVER_INDEXED);
						pl += he.encode(f, ii, true, HeaderField.Representation.WITHOUT_INDEXING);
						break;
					case "date": // , "x-api-key":
						pl += he.encode(f, ii, true, HeaderField.Representation.NEVER_INDEXED);
						break;
					default:
//						pl += he.encode(f, ii, true, HeaderField.Representation.WITHOUT_INDEXING);
						pl += he.encode(f, ii, true, HeaderField.Representation.WITH_INDEXING);
						break;
					}
				}
				var i = ii.build().iterator();
				var bb2 = new byte[pl];
				for (var j = 0; j < pl; j++)
					bb2[j] = (byte) i.nextInt();
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Frame.Name.HEADERS.type());
				bb.put((byte) ((x.endHeaders() ? 0x04 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
				bb.putInt(x.streamIdentifier());
				bb.flip();
				channel.write(bb);
				channel.write(ByteBuffer.wrap(bb2));
				break;
			case Frame.Ping x:
				throw new RuntimeException();
			case Frame.Priority x:
				throw new RuntimeException();
			case Frame.RstStream x:
				throw new RuntimeException();
			case Frame.Settings x:
				pl = x.parameters().size() * (Short.BYTES + Integer.BYTES);
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Frame.Name.SETTINGS.type());
				bb.put((byte) (x.ack() ? 0x01 : 0x00));
				bb.putInt(0);
				bb.flip();
				channel.write(bb);
				bb = ByteBuffer.allocate(pl);
				for (var y : x.parameters()) {
					bb.putShort((short) y.name().identifier());
					bb.putInt(y.value());
				}
				bb.flip();
				channel.write(bb);
				break;
			case Frame.WindowUpdate x:
				throw new RuntimeException();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Frame decode(ReadableByteChannel channel, HeaderDecoder hd) {
		ByteBuffer bb1, bb2;
		try {
			bb1 = ByteBuffer.allocate(9);
			var n = channel.read(bb1);
//			System.out.println("n=" + n);
			if (n < 9)
				return null;
			bb1.flip();
			var pl = (Short.toUnsignedInt(bb1.getShort()) << 8) | Byte.toUnsignedInt(bb1.get());
//			System.out.println("pl=" + pl);
			bb2 = ByteBuffer.allocate(pl);
			if (pl > 0) {
				n = channel.read(bb2);
//				System.out.println("n=" + n);
				bb2.flip();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

//		buffer.position(aa);
//		System.out.println(Util.toHexString(IO.toIntStream(buffer)));
//		buffer.position(aa + Short.BYTES + Byte.BYTES);

//		System.out.println("pl=" + pl);
		var t0 = Byte.toUnsignedInt(bb1.get());
		var t = Frame.Name.of(t0);
		if (t == null)
			System.out.println("t0=" + t0 + ", t=" + t);
		var ff = Byte.toUnsignedInt(bb1.get());
//		System.out.println("ff=" + ff);
		var si = bb1.getInt() & 0x8fffffff;
//		System.out.println("si=" + si);
		var f = switch (t) {
		case DATA -> {
			yield new Frame.Data((ff & 0x08) != 0, (ff & 0x01) != 0, si, bb2.array());
		}
		case GOAWAY -> {
			var lsi = bb2.getInt() & 0x8fffffff;
			var ec = bb2.getInt();
			var add = Arrays.copyOfRange(bb2.array(), bb2.position(), bb2.limit());
			yield new Frame.Goaway(lsi, ec, add);
		}
		case HEADERS -> {
			var p = (ff & 0x20) != 0;
			boolean e;
			int sd, w;
			if (p) {
				var i = bb2.getInt();
				e = (i & 0x80000000) != 0;
				sd = i & 0x7fffffff;
				w = Byte.toUnsignedInt(bb2.get());
			} else {
				e = false;
				sd = 0;
				w = 0;
			}
			hd.headerFields().clear();
			var ii = IO.toIntStream(bb2).iterator();
			while (ii.hasNext())
				hd.decode(ii);
//			System.out.println("hd.headerFields=" + hd.headerFields());
			yield new Frame.Headers(p, (ff & 0x04) != 0, (ff & 0x01) != 0, si, e, sd, w,
					new ArrayList<>(hd.headerFields()));
		}
		case PING -> {
			var od = bb2.getLong();
			yield new Frame.Ping((ff & 0x01) != 0, si, od);
		}
		case PRIORITY -> {
			var i = bb2.getInt();
			var e = (i & 0xf0000000) != 0;
			var sd = i & 0x8fffffff;
			var w = Byte.toUnsignedInt(bb2.get());
			yield new Frame.Priority(si, e, sd, w);
		}
		case RST_STREAM -> {
			var ec = bb2.getInt();
			yield new Frame.RstStream(si, ec);
		}
		case SETTINGS -> {
			var pp = new ArrayList<Setting.Parameter>();
			while (bb2.hasRemaining())
				pp.add(new Setting.Parameter(Setting.Name.of(bb2.getShort()), bb2.getInt()));
			yield new Frame.Settings((ff & 0x01) != 0, pp);
		}
		case WINDOW_UPDATE -> {
			var wsi = bb2.getInt() & 0x8fffffff;
//			System.out.println("wsi=" + wsi);
			yield new Frame.WindowUpdate(si, wsi);
		}
		default -> throw new RuntimeException(t.name());
		};
		return f;
	}
}
