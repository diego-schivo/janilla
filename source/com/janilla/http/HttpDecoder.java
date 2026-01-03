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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class HttpDecoder {

	protected final HeaderDecoder headerDecoder = new HeaderDecoder();

	public Frame decodeFrame(byte[] bytes) {
		var bb = ByteBuffer.wrap(bytes);
		var pl = (Short.toUnsignedInt(bb.getShort()) << 8) | Byte.toUnsignedInt(bb.get());
//		IO.println("HttpDecoder.decodeFrame, pl=" + pl);
		if (pl != bytes.length - 9)
			throw new RuntimeException();
		var t0 = Byte.toUnsignedInt(bb.get());
		var fn = FrameName.of(t0);
//		if (fn == null)
//			IO.println("HttpDecoder.decodeFrame, t0=" + t0 + ", t=" + fn);
		var ff = Byte.toUnsignedInt(bb.get());
//		IO.println("HttpDecoder.decodeFrame, ff=" + ff);
		var si = bb.getInt() & 0x8fffffff;
//		IO.println("si=" + si);
		var f = switch (fn) {
		case DATA -> {
			yield new DataFrame((ff & 0x08) != 0, (ff & 0x01) != 0, si,
					Arrays.copyOfRange(bb.array(), bb.position(), bb.limit()));
		}
		case GOAWAY -> {
			var lsi = bb.getInt() & 0x8fffffff;
			var ec = bb.getInt();
			var add = Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
			yield new GoawayFrame(lsi, ec, add);
		}
		case HEADERS -> {
			var p = (ff & 0x20) != 0;
			boolean e;
			int sd, w;
			if (p) {
				var i = bb.getInt();
				e = (i & 0x80000000) != 0;
				sd = i & 0x7fffffff;
				w = Byte.toUnsignedInt(bb.get());
			} else {
				e = false;
				sd = 0;
				w = 0;
			}
			headerDecoder.headerFields().clear();
			var ii = IntStream.generate(() -> bb.hasRemaining() ? Byte.toUnsignedInt(bb.get()) : -1)
					.takeWhile(x -> x != -1).iterator();
			while (ii.hasNext())
				headerDecoder.decode(ii);
//			IO.println("HttpDecoder.decodeFrame, hd.headerFields=" + hd.headerFields());
			yield new HeadersFrame(p, (ff & 0x04) != 0, (ff & 0x01) != 0, si, e, sd, w,
					new ArrayList<>(headerDecoder.headerFields()));
		}
		case PING -> {
			var od = bb.getLong();
			yield new PingFrame((ff & 0x01) != 0, si, od);
		}
		case PRIORITY -> {
			var i = bb.getInt();
			var e = (i & 0xf0000000) != 0;
			var sd = i & 0x8fffffff;
			var w = Byte.toUnsignedInt(bb.get());
			yield new PriorityFrame(si, e, sd, w);
		}
		case RST_STREAM -> {
			var ec = bb.getInt();
			yield new RstStreamFrame(si, ec);
		}
		case SETTINGS -> {
			var pp = new ArrayList<SettingParameter>();
			while (bb.hasRemaining())
				pp.add(new SettingParameter(SettingName.of(bb.getShort()), bb.getInt()));
			yield new SettingsFrame((ff & 0x01) != 0, pp);
		}
		case WINDOW_UPDATE -> {
			var wsi = bb.getInt() & 0x8fffffff;
//			IO.println("HttpDecoder.decodeFrame, wsi=" + wsi);
			yield new WindowUpdateFrame(si, wsi);
		}
		default -> throw new RuntimeException(fn.name());
		};
//		IO.println("HttpDecoder.decodeFrame, f=" + f);
		return f;
	}
}
