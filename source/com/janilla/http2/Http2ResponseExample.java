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
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.hpack.HeaderField;
import com.janilla.util.Util;

public class Http2ResponseExample {

	public static void main(String[] args) {
		try {
			var s1 = """
					0000 1204 0000 0000 0000 0100 0010 0000
					0500 0040 0000 0400 00ff ff00 002e 0104
					0000 0001 3fe1 7f88 5f0a 7465 7874 2f70
					6c61 696e 1f0d 0231 311f 1296 d07a be94
					134a 65b6 a504 0134 a05b b826 6e34 2531
					68df 0000 0b00 0100 0000 0148 656c 6c6f
					2057 6f72 6c64""";
			var ii = Util.parseHex(s1).iterator();
			var br = new BitsReader(ii);
			var hd = new HeaderDecoder();
			while (br.hasNext()) {
				var l = br.nextInt(24);
				System.out.println("l=" + l);
				var t = FrameName.of(br.nextInt(8));
				System.out.println("t=" + t);
				var ff = switch (t) {
				case DATA -> Stream
						.of(br.nextInt(4) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
								br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.END_STREAM : null)
						.filter(x -> x != null).toList();
				case HEADERS -> Stream.of(br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PRIORITY : null,
						br.nextInt(1) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
						br.nextInt(1) != 0 ? Flag.END_HEADERS : null, br.nextInt(1) != 0 ? null : null,
						br.nextInt(1) != 0 ? Flag.END_STREAM : null).filter(x -> x != null).toList();
				case SETTINGS -> Stream.of(br.nextInt(7) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.ACK : null)
						.filter(x -> x != null).toList();
				default -> throw new RuntimeException();
				};
				System.out.println("ff=" + ff);
				var r = br.nextInt(1);
				System.out.println("r=" + r);
				var si = br.nextInt(31);
				System.out.println("si=" + si);
				switch (t) {
				case DATA:
					var cc = new byte[l];
					for (var i = 0; i < l; i++)
						cc[i] = (byte) ii.nextInt();
					System.out.println(new String(cc));
					break;
				case HEADERS:
					var br2 = new BitsReader(new LengthIntIterator(ii, l));
					while (br2.hasNext())
						hd.decode(br2);
					var hh = hd.headerFields();
					System.out.println("hh=" + hh);
					break;
				case SETTINGS:
					for (var z = 0; z < l; z += Short.BYTES + Integer.BYTES) {
						var i = br.nextInt(16);
						var s = SettingName.of(i);
						var v = br.nextInt(32);
						System.out.println(s + "=" + v);
					}
					break;
				default:
					throw new RuntimeException();
				}
			}

			var baos = new ByteArrayOutputStream();
			var bw = new BitsWriter(new ByteWriter(baos));
			FrameEncoder f;

			f = new FrameEncoder(FrameName.SETTINGS, bw);
			f.encodeLength(18);
			f.encodeType();
			f.encodeFlags(Set.of());
			f.encodeReserved();
			f.encodeStreamIdentifier(0);
			f.encodeSetting(new Setting(SettingName.HEADER_TABLE_SIZE, 4096));
			f.encodeSetting(new Setting(SettingName.MAX_FRAME_SIZE, 16384));
			f.encodeSetting(new Setting(SettingName.INITIAL_WINDOW_SIZE, 65535));

			f = new FrameEncoder(FrameName.HEADERS, bw);
			f.encodeLength(46);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.END_HEADERS));
			f.encodeReserved();
			f.encodeStreamIdentifier(1);
			f.headerEncoder().encodeDynamicTableSizeUpdate(16384);
			f.headerEncoder().encode(new HeaderField(":status", "200"));
			f.headerEncoder().encode(new HeaderField("content-type", "text/plain"));
			f.headerEncoder().encode(new HeaderField("content-length", "11"), false,
					HeaderField.Representation.NEVER_INDEXED);
			f.headerEncoder().encode(new HeaderField("date", "Mon, 24 Jun 2024 15:23:42 GMT"), true,
					HeaderField.Representation.NEVER_INDEXED);

			f = new FrameEncoder(FrameName.DATA, bw);
			f.encodeLength(11);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.END_STREAM));
			f.encodeReserved();
			f.encodeStreamIdentifier(1);
			baos.write("Hello World".getBytes());

			var s2 = Util.toHexString(Util.toIntStream(baos.toByteArray()));
			System.out.println(s2);
			assert s2.equals(s1) : s2;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
