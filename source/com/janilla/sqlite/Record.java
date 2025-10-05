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
package com.janilla.sqlite;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public class Record {

	public static byte[] toBytes(Object... values) {
		var tt = new int[values.length];
		var vv = new ArrayList<byte[]>(values.length);
		var i = 0;
		for (var v : values) {
			if (v == null) {
				tt[i] = 0;
				vv.add(new byte[0]);
			} else if (v instanceof Number x) {
				if (x.intValue() == 0) {
					tt[i] = 8;
					vv.add(new byte[0]);
				} else if (x.intValue() == 1) {
					tt[i] = 9;
					vv.add(new byte[0]);
				} else if (x.intValue() < 128) {
					tt[i] = 1;
					vv.add(new byte[] { (byte) x.intValue() });
				} else {
					tt[i] = 2;
					vv.add(new byte[] { (byte) (x.intValue() >>> 8), (byte) x.intValue() });
				}
			} else if (v instanceof String s) {
				tt[i] = s.length() * 2 + 13;
				vv.add(s.getBytes());
			} else
				throw new RuntimeException(Objects.toString(v));
			i++;
		}
		var s = Arrays.stream(tt).map(Varint::size).sum();
		var s1 = Varint.size(s);
		var s2 = Varint.size(s1 + s);
		var bb = new byte[(s2 != s1 ? Varint.size(s2 + s) : s2) + s + vv.stream().mapToInt(x -> x.length).sum()];
		var b = ByteBuffer.wrap(bb);
		Varint.put(b, s2 + s);
		for (var x : tt)
			Varint.put(b, x);
		for (var x : vv)
			b.put(x);
		return bb;
	}

	public static Object[] fromBytes(byte[] bb) {
		var b = ByteBuffer.wrap(bb);
		var p = Varint.get(b);
		var b2 = IntStream.builder();
		while (b.position() != p)
			b2.add((int) Varint.get(b));
		var tt = b2.build().toArray();
		return Arrays.stream(tt).mapToObj(x -> {
			return switch (x) {
			case 0 -> null;
			case 1 -> Byte.toUnsignedLong(b.get());
			case 2 -> Short.toUnsignedLong(b.getShort());
			case 8 -> 0l;
			case 9 -> 1l;
			default -> {
				var bb2 = new byte[(x - 13) / 2];
				b.get(bb2);
				yield new String(bb2);
			}
			};
		}).toArray();
	}
}
