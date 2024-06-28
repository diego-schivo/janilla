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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.hpack.HeaderField;
import com.janilla.util.Util;

public class Http2TlsResponseExample {

	public static void main(String[] args) {
		try {
			var s1 = Util.toHexString(Stream.of("""
					0000 1204 0000 0000 0000 0100 0010 0000
					0500 0040 0000 0400 00ff ff""", """
					0000 0004 0100 0000 00""", """
					0000 2f01 0400 0000 013f e17f 885f 0a74
					6578 742f 706c 6169 6e1f 0d02 3131 1f12
					97c3 61be 9413 ca65 b6a5 0401 34a0 5ab8
					d337 1a79 4c5a 37ff 0000 0b00 0100 0000
					0148 656c 6c6f 2057 6f72 6c64""").map(Util::parseHex).reduce(IntStream.empty(), IntStream::concat));
			System.out.println(s1);
			var ii = Util.parseHex(s1).iterator();
			var br = new BitsReader(ii);
			var hd = new HeaderDecoder();
			while (br.hasNext()) {
				var l = br.nextInt(24);
				System.out.println("l=" + l);
				var t = FrameName.of(br.nextInt(8));
				System.out.println("t=" + t);
				{
					var ff = switch (t) {
					case DATA -> Stream
							.of(br.nextInt(4) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
									br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.END_STREAM : null)
							.filter(x -> x != null).collect(Collectors.toSet());
					case HEADERS -> Stream
							.of(br.nextInt(2) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PRIORITY : null,
									br.nextInt(1) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.PADDED : null,
									br.nextInt(1) != 0 ? Flag.END_HEADERS : null, br.nextInt(1) != 0 ? null : null,
									br.nextInt(1) != 0 ? Flag.END_STREAM : null)
							.filter(x -> x != null).collect(Collectors.toSet());
					case SETTINGS -> Stream.of(br.nextInt(7) != 0 ? null : null, br.nextInt(1) != 0 ? Flag.ACK : null)
							.filter(x -> x != null).collect(Collectors.toSet());
					case WINDOW_UPDATE -> {
						br.nextInt();
						yield Set.of();
					}
					default -> throw new RuntimeException();
					};
					System.out.println("ff=" + ff);
					var r = br.nextInt(1);
					System.out.println("r=" + r);
					var si = br.nextInt(31);
					System.out.println("si=" + si);
				}
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
					var ss = hd.dynamicTableMaxSizes();
					System.out.println("ss=" + ss);
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
				case WINDOW_UPDATE:
					var r = br.nextInt(1);
					System.out.println("r=" + r);
					var wsi = br.nextInt(31);
					System.out.println("wsi=" + wsi);
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
			
			System.err.println(Util.toHexString(Util.toIntStream(baos.toByteArray())));

			f = new FrameEncoder(FrameName.SETTINGS, bw);
			f.encodeLength(0);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.ACK));
			f.encodeReserved();
			f.encodeStreamIdentifier(0);

			f = new FrameEncoder(FrameName.HEADERS, bw);
			f.encodeLength(47);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.END_HEADERS));
			f.encodeReserved();
			f.encodeStreamIdentifier(1);
			f.headerEncoder().encodeDynamicTableSizeUpdate(16384);
			f.headerEncoder().encode(new HeaderField(":status", "200"));
			f.headerEncoder().encode(new HeaderField("content-type", "text/plain"));
			f.headerEncoder().encode(new HeaderField("content-length", "11"), false,
					HeaderField.Representation.NEVER_INDEXED);
			f.headerEncoder().encode(new HeaderField("date", "Fri, 28 Jun 2024 14:43:48 GMT"), true,
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
