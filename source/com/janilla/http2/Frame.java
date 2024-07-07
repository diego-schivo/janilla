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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.hpack.Representation;
import com.janilla.io.IO;
import com.janilla.util.BitsConsumer;
import com.janilla.util.BitsIterator;

sealed interface Frame permits DataFrame, GoawayFrame, HeadersFrame, PriorityFrame, SettingsFrame, WindowUpdateFrame {

	int streamIdentifier();

	static void encode(Frame frame, WritableByteChannel channel) {
		var baos = new ByteArrayOutputStream();
		var bw = new BitsConsumer(baos::write);
		FrameEncoder fe;
		switch (frame) {
		case DataFrame x:
			fe = new FrameEncoder(FrameName.DATA, bw);
			fe.encodeLength(x.data().length);
			fe.encodeType();
			fe.encodeFlags(Stream.of(x.padded() ? Flag.PADDED : null, x.endStream() ? Flag.END_STREAM : null)
					.filter(y -> y != null).collect(Collectors.toSet()));
			fe.encodeReserved();
			fe.encodeStreamIdentifier(x.streamIdentifier());
			try {
				baos.write(x.data());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			break;
		case GoawayFrame x:
			break;
		case HeadersFrame x:
			fe = new FrameEncoder(FrameName.HEADERS, bw);
			for (var hf : x.fields()) {
				switch (hf.name()) {
				case ":method", ":scheme", ":status", "content-type":
					fe.headerEncoder().encode(hf);
					break;
				case "content-length":
					fe.headerEncoder().encode(hf, false, Representation.NEVER_INDEXED);
					break;
				case "date":
					fe.headerEncoder().encode(hf, true, Representation.NEVER_INDEXED);
					break;
				default:
					fe.headerEncoder().encode(hf, true, Representation.WITHOUT_INDEXING);
					break;
				}
			}
			var p = baos.toByteArray();
			baos.reset();
			fe.encodeLength(p.length);
			fe.encodeType();
			fe.encodeFlags(Stream.of(x.endHeaders() ? Flag.END_HEADERS : null, x.endStream() ? Flag.END_STREAM : null)
					.filter(y -> y != null).collect(Collectors.toSet()));
			fe.encodeReserved();
			fe.encodeStreamIdentifier(x.streamIdentifier());
			try {
				baos.write(p);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			break;
		case PriorityFrame x:
			throw new RuntimeException();
		case SettingsFrame x:
			fe = new FrameEncoder(FrameName.SETTINGS, bw);
			fe.encodeLength(x.parameters().size() * (Short.BYTES + Integer.BYTES));
			fe.encodeType();
			fe.encodeFlags(x.acknowledge() ? Set.of(Flag.ACK) : null);
			fe.encodeReserved();
			fe.encodeStreamIdentifier(0);
			for (var e : x.parameters().entrySet())
				fe.encodeSetting(new Setting(e.getKey(), e.getValue()));
			break;
		case WindowUpdateFrame x:
			throw new RuntimeException();
		}
		try {
			var bb = baos.toByteArray();
//			System.out.println("bb=\n" + Util.toHexString(Util.toIntStream(bb)));
			IO.write(bb, channel);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static Frame decode(ReadableByteChannel channel, HeaderDecoder hd) {
		var ii = IO.toIntStream(channel).iterator();
//		if (!ii.hasNext())
//			return null;
		var br = new BitsIterator(ii);
		var l = br.nextInt(24);
//		System.out.println("l=" + l);
		var t = FrameName.of(br.nextInt(8));
//		System.out.println("t=" + t);
		var ff = switch (t) {
		case DATA -> Stream
				.of(br.nextInt(4) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
						br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.END_STREAM : null)
				.filter(x -> x != null).collect(Collectors.toSet());
		case GOAWAY -> {
			br.nextInt();
			yield Set.of();
		}
		case HEADERS -> Stream.of(br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PRIORITY : null,
				br.nextInt(1) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
				br.nextInt(1) != 0 ? Flag.END_HEADERS : null, br.nextInt(1) != 0 ? null : null,
				br.nextInt(1) != 0 ? Flag.END_STREAM : null).filter(x -> x != null).collect(Collectors.toSet());
		case PRIORITY -> {
			br.nextInt();
			yield Set.of();
		}
		case SETTINGS -> Stream.of(br.nextInt(7) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.ACK : null)
				.filter(x -> x != null).collect(Collectors.toSet());
		case WINDOW_UPDATE -> {
			br.nextInt();
			yield Set.of();
		}
		default -> throw new RuntimeException("t=" + t);
		};
//		System.out.println("ff=" + ff);
		br.nextInt(1);
		var si = br.nextInt(31);
//		System.out.println("si=" + si);
		return switch (t) {
		case DATA -> {
			var d = new byte[l];
			try {
				if (IO.read(channel, d) < d.length)
					throw new RuntimeException();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			yield new DataFrame(ff.contains(Flag.PADDED), ff.contains(Flag.END_STREAM), si, d);
		}
		case GOAWAY -> {
			br.nextInt(1);
			var lsi = br.nextInt(31);
			var ec = br.nextInt(32);
			var add = new byte[l - 2 * Integer.BYTES];
			try {
				if (IO.read(channel, add) < add.length)
					throw new RuntimeException();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			yield new GoawayFrame(lsi, ec, add);
		}
		case HEADERS -> {
			var p = ff.contains(Flag.PRIORITY);
			boolean e;
			int sd, w;
			if (p) {
				e = br.nextInt(1) == 0x01;
				sd = br.nextInt(31);
				w = br.nextInt(8);
			} else {
				e = false;
				sd = 0;
				w = 0;
			}
			class LengthIntIterator implements PrimitiveIterator.OfInt {

				PrimitiveIterator.OfInt iterator;

				int length;

				int count;

				LengthIntIterator(PrimitiveIterator.OfInt iterator, int length) {
					this.iterator = iterator;
					this.length = length;
				}

				@Override
				public boolean hasNext() {
					return count < length && iterator.hasNext();
				}

				@Override
				public int nextInt() {
					var i = iterator.nextInt();
					count++;
					return i;
				}
			}
			var br2 = new BitsIterator(new LengthIntIterator(ii, l - (p ? Integer.BYTES + Byte.BYTES : 0)));
			hd.headerFields().clear();
			while (br2.hasNext())
				hd.decode(br2);
			System.out.println("hd.headerFields=" + hd.headerFields());
			yield new HeadersFrame(p, ff.contains(Flag.END_HEADERS), ff.contains(Flag.END_STREAM), si, e, sd, w,
					new ArrayList<>(hd.headerFields()));
		}
		case PRIORITY -> {
			var e = br.nextInt(1) == 0x01;
			var sd = br.nextInt(31);
			var w = br.nextInt(8);
			yield new PriorityFrame(si, e, sd, w);
		}
		case SETTINGS -> {
			var a = ff.contains(Flag.ACK);
			var pp = Stream.generate(() -> null).limit(l / (Short.BYTES + Integer.BYTES))
					.collect(Collectors.toMap(x -> SettingName.of(br.nextInt(16)), x -> br.nextInt(32)));
			yield new SettingsFrame(a, pp);
		}
		case WINDOW_UPDATE -> {
			br.nextInt(1);
			var wsi = br.nextInt(31);
//			System.out.println("wsi=" + wsi);
			yield new WindowUpdateFrame(si, wsi);
		}
		default -> throw new RuntimeException();
		};
	}
}
