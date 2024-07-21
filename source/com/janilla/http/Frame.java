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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.hpack.HeaderEncoder;
import com.janilla.hpack.Representation;
import com.janilla.io.IO;
import com.janilla.media.HeaderField;

sealed interface Frame permits Frame.Data, Frame.Goaway, Frame.Headers, Frame.Ping, Frame.Priority, Frame.RstStream,
		Frame.Settings, Frame.WindowUpdate {

	static void encode(Frame frame, WritableByteChannel channel) {
		try {
			int pl;
			var bb = ByteBuffer.allocate(9);
			switch (frame) {
			case Data x:
				pl = x.data().length;
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Name.DATA.type());
				bb.put((byte) ((x.padded() ? 0x08 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
				bb.putInt(x.streamIdentifier());
				bb.flip();
				channel.write(bb);
				channel.write(ByteBuffer.wrap(x.data()));
				break;
			case Goaway x:
				throw new RuntimeException();
			case Headers x:
				var he = new HeaderEncoder();
				var ii = IntStream.builder();
				pl = 0;
				for (var hf : x.fields()) {
					switch (hf.name()) {
					case ":method", ":scheme", ":status", "content-type":
						pl += he.encode(hf, ii);
						break;
					case "content-length":
						pl += he.encode(hf, ii, false, Representation.NEVER_INDEXED);
						break;
					case "date":
						pl += he.encode(hf, ii, true, Representation.NEVER_INDEXED);
						break;
					default:
						pl += he.encode(hf, ii, true, Representation.WITHOUT_INDEXING);
						break;
					}
				}
				var i = ii.build().iterator();
				var bb2 = new byte[pl];
				for (var j = 0; j < pl; j++)
					bb2[j] = (byte) i.nextInt();
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Name.HEADERS.type());
				bb.put((byte) ((x.endHeaders() ? 0x04 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
				bb.putInt(x.streamIdentifier());
				bb.flip();
				channel.write(bb);
				channel.write(ByteBuffer.wrap(bb2));
				break;
			case Ping x:
				throw new RuntimeException();
			case Priority x:
				throw new RuntimeException();
			case RstStream x:
				throw new RuntimeException();
			case Settings x:
				pl = x.parameters().size() * (Short.BYTES + Integer.BYTES);
				bb.putShort((short) ((pl >>> 8) & 0xffff));
				bb.put((byte) pl);
				bb.put((byte) Name.SETTINGS.type());
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
			case WindowUpdate x:
				throw new RuntimeException();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static Frame decode(ReadableByteChannel channel, HeaderDecoder hd) {
		ByteBuffer bb1, bb2;
		try {
			bb1 = ByteBuffer.allocate(9);
			if (channel.read(bb1) < 9)
				return null;
			bb1.flip();
			var pl = (Short.toUnsignedInt(bb1.getShort()) << 8) | Byte.toUnsignedInt(bb1.get());
			bb2 = ByteBuffer.allocate(pl);
			channel.read(bb2);
			bb2.flip();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

//		buffer.position(aa);
//		System.out.println(Util.toHexString(IO.toIntStream(buffer)));
//		buffer.position(aa + Short.BYTES + Byte.BYTES);

//		System.out.println("pl=" + pl);
		var t0 = Byte.toUnsignedInt(bb1.get());
		var t = Name.of(t0);
		if (t == null)
			System.out.println("t0=" + t0 + ", t=" + t);
		var ff = Byte.toUnsignedInt(bb1.get());
//		System.out.println("ff=" + ff);
		var si = bb1.getInt() & 0x8fffffff;
//		System.out.println("si=" + si);
		var f = switch (t) {
		case DATA -> {
			yield new Data((ff & 0x08) != 0, (ff & 0x01) != 0, si, bb2.array());
		}
		case GOAWAY -> {
			var lsi = bb2.getInt() & 0x8fffffff;
			var ec = bb2.getInt();
			var add = Arrays.copyOfRange(bb2.array(), bb2.position(), bb2.limit());
			yield new Goaway(lsi, ec, add);
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
			yield new Headers(p, (ff & 0x04) != 0, (ff & 0x01) != 0, si, e, sd, w, new ArrayList<>(hd.headerFields()));
		}
		case PING -> {
			var od = bb2.getLong();
			yield new Ping((ff & 0x01) != 0, si, od);
		}
		case PRIORITY -> {
			var i = bb2.getInt();
			var e = (i & 0xf0000000) != 0;
			var sd = i & 0x8fffffff;
			var w = Byte.toUnsignedInt(bb2.get());
			yield new Priority(si, e, sd, w);
		}
		case RST_STREAM -> {
			var ec = bb2.getInt();
			yield new RstStream(si, ec);
		}
		case SETTINGS -> {
			var pp = new ArrayList<Setting.Parameter>();
			while (bb2.hasRemaining())
				pp.add(new Setting.Parameter(Setting.Name.of(bb2.getShort()), bb2.getInt()));
			yield new Settings((ff & 0x01) != 0, pp);
		}
		case WINDOW_UPDATE -> {
			var wsi = bb2.getInt() & 0x8fffffff;
//			System.out.println("wsi=" + wsi);
			yield new WindowUpdate(si, wsi);
		}
		default -> throw new RuntimeException(t.name());
		};
		return f;
	}

	int streamIdentifier();

	enum Name {

		DATA(0x00), HEADERS(0x01), PRIORITY(0x02), RST_STREAM(0x03), SETTINGS(0x04), PUSH_PROMISE(0x05), PING(0x06),
		GOAWAY(0x07), WINDOW_UPDATE(0x08), CONTINUATION(0x09);

		static Name[] array;

		static {
			var t = Arrays.stream(values()).mapToInt(Name::type).max().getAsInt();
			array = new Name[t + 1];
			for (var f : values())
				array[f.type()] = f;
		}

		static Name of(int type) {
			return 0 <= type && type < array.length ? array[type] : null;
		}

		int type;

		Name(int type) {
			this.type = type;
		}

		int type() {
			return type;
		}
	}

	record Data(boolean padded, boolean endStream, int streamIdentifier, byte[] data) implements Frame {
	}

	record Goaway(int lastStreamId, int errorCode, byte[] additionalDebugData) implements Frame {

		@Override
		public int streamIdentifier() {
			return 0;
		}
	}

	record Headers(boolean priority, boolean endHeaders, boolean endStream, int streamIdentifier, boolean exclusive,
			int streamDependency, int weight, Iterable<HeaderField> fields) implements Frame {
	}

	record Ping(boolean ack, int streamIdentifier, Long opaqueData) implements Frame {
	}

	record Priority(int streamIdentifier, boolean exclusive, int streamDependency, int weight) implements Frame {
	}

	record RstStream(int streamIdentifier, int errorCode) implements Frame {
	}

	record Settings(boolean ack, List<Setting.Parameter> parameters) implements Frame {

		@Override
		public int streamIdentifier() {
			return 0;
		}
	}

	record WindowUpdate(int streamIdentifier, int windowSizeIncrement) implements Frame {
	}
}
