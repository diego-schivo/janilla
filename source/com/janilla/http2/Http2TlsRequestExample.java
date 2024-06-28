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

public class Http2TlsRequestExample {

	public static void main(String[] args) {
		try {
			var s1 = Util.toHexString(Stream.of("""
					5052 4920 2a20 4854 5450 2f32 2e30 0d0a
					0d0a 534d 0d0a 0d0a 0000 1e04 0000 0000
					0000 0100 0040 0000 0200 0000 0100 0300
					0000 6400 0401 0000 0000 0500 0040 0000
					0004 0800 0000 0000 01ff 0001 0000 2701
					0500 0000 0120 018a a0e4 1d13 9d09 b8f3
					4d33 8204 8362 539f 870f 2b90 ca3e e35a
					74a6 b589 418b 5258 1097 02e1""", """
					0000 0004 0100 0000 00""", """
					0000 2601 0500 0000 0301 8aa0 e41d 139d
					09b8 f34d 3382 0583 6231 d987 0f2b 90ca
					3ee3 5a74 a6b5 8941 8b52 5810 9702 e1""").map(Util::parseHex).reduce(IntStream.empty(),
					IntStream::concat));
			var ii = Util.parseHex(s1).iterator();
			var br = new BitsReader(ii);
			var hd = new HeaderDecoder();
			{
				var sb = new StringBuilder();
				for (var i = 0; i < 24; i++)
					sb.append((char) br.nextInt());
				var s = sb.toString();
				System.out.println("s=" + s);
				assert s.equals(Http2.CLIENT_CONNECTION_PREFACE_PREFIX) : s;
			}
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
					ss.clear();
					var hh = hd.headerFields();
					System.out.println("hh=" + hh);
					hh.clear();
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

			baos.write(Http2.CLIENT_CONNECTION_PREFACE_PREFIX.getBytes());

			f = new FrameEncoder(FrameName.SETTINGS, bw);
			f.encodeLength(30);
			f.encodeType();
			f.encodeFlags(Set.of());
			f.encodeReserved();
			f.encodeStreamIdentifier(0);
			f.encodeSetting(new Setting(SettingName.HEADER_TABLE_SIZE, 16384));
			f.encodeSetting(new Setting(SettingName.ENABLE_PUSH, 1));
			f.encodeSetting(new Setting(SettingName.MAX_CONCURRENT_STREAMS, 100));
			f.encodeSetting(new Setting(SettingName.INITIAL_WINDOW_SIZE, 16777216));
			f.encodeSetting(new Setting(SettingName.MAX_FRAME_SIZE, 16384));

			f = new FrameEncoder(FrameName.WINDOW_UPDATE, bw);
			f.encodeLength(4);
			f.encodeType();
			f.encodeFlags(Set.of());
			f.encodeReserved();
			f.encodeStreamIdentifier(0);
			f.encodeReserved();
			f.encodeWindowSizeIncrement(33488897);

			f = new FrameEncoder(FrameName.HEADERS, bw);
			f.encodeLength(39);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.END_HEADERS, Flag.END_STREAM));
			f.encodeReserved();
			f.encodeStreamIdentifier(1);
			f.headerEncoder().encodeDynamicTableSizeUpdate(0);
			f.headerEncoder().encode(new HeaderField(":authority", "localhost:8443"), true,
					HeaderField.Representation.WITHOUT_INDEXING);
			f.headerEncoder().encode(new HeaderField(":method", "GET"));
			f.headerEncoder().encode(new HeaderField(":path", "/foo"), true,
					HeaderField.Representation.WITHOUT_INDEXING);
			f.headerEncoder().encode(new HeaderField(":scheme", "https"));
			f.headerEncoder().encode(new HeaderField("user-agent", "Java-http-client/22.0.1"), true,
					HeaderField.Representation.WITHOUT_INDEXING);

			f = new FrameEncoder(FrameName.SETTINGS, bw);
			f.encodeLength(0);
			f.encodeType();
			f.encodeFlags(Set.of(Flag.ACK));
			f.encodeReserved();
			f.encodeStreamIdentifier(0);

			var s2 = Util.toHexString(Util.toIntStream(baos.toByteArray()));
			System.out.println(s2);
			assert s2.equals(s1) : s2;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
