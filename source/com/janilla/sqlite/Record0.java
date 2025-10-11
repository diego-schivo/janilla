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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Record0 {

	public static byte[] toBytes(Object... values) {
		var tt = new int[values.length];
		var vv = new ArrayList<byte[]>(values.length);
		var i = 0;
		for (var v : values) {
			if (v == null) {
				tt[i] = 0;
				vv.add(new byte[0]);
			} else if (v instanceof Long x) {
				long l = x;
				switch (l) {
				case 0l:
					tt[i] = 8;
					vv.add(new byte[0]);
					break;
				case 1l:
					tt[i] = 9;
					vv.add(new byte[0]);
					break;
				default:
					var ni = 0;
					var nn = new int[] { 8, 16, 24, 32, 48, 64 };
					for (; ni < nn.length; ni++) {
						var m = (1l << nn[ni]) - 1;
						if ((l & m) == l)
							break;
					}
					tt[i] = 1 + ni;
					var b = ByteBuffer.allocate(Long.BYTES);
					b.putLong(l);
					var bb = Arrays.copyOfRange(b.array(), Long.BYTES - nn[ni] / 8, Long.BYTES);
					vv.add(bb);
					break;
				}
			} else if (v instanceof Double x) {
				tt[i] = 7;
				var b = ByteBuffer.allocate(Double.BYTES);
				b.putDouble(x);
				vv.add(b.array());
			} else if (v instanceof String s) {
				var bb = s.getBytes();
				tt[i] = bb.length * 2 + 13;
				vv.add(bb);
			} else
				throw new RuntimeException(Objects.toString(v) + " (" + v.getClass() + ")");
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

	public static Stream<Object> fromBytes(Stream<ByteBuffer> buffers) {
		var bi = buffers.iterator();
		var b = new ByteBuffer[] { bi.next() };
		long p = Varint.get(b[0]);
		int[] tt;
		{
			var ii = IntStream.builder();
			while (b[0].position() != p)
				ii.add((int) Varint.get(b[0]));
			tt = ii.build().toArray();
		}
//		IO.println("tt=" + Arrays.toString(tt));
		return Arrays.stream(tt).mapToObj(x -> {
			return switch (x) {
			case 0 -> null;
			case 1, 2, 3, 4, 5, 6 -> {
				var n = (new int[] { 8, 16, 24, 32, 48, 64 })[x - 1];
				var b2 = ByteBuffer.allocate(Long.BYTES);
				b[0].get(b2.array(), Long.BYTES - n / 8, n / 8);
				var l = b2.getLong();
//				IO.println("n=" + n + ", l=" + l);
				yield l;
			}
			case 7 -> {
				var b2 = ByteBuffer.allocate(Double.BYTES);
				b[0].get(b2.array());
				var d = b2.getDouble();
				yield d;
			}
			case 8 -> 0l;
			case 9 -> 1l;
			case 13 -> "";
			default -> {
				if (x >= 15 && x % 2 == 1) {
					var bb2 = new byte[(x - 13) / 2];
//				IO.println("bb2=" + bb2.length);
					for (;;)
						try {
//						IO.println("b[0]=" + b[0].remaining());
							b[0].get(bb2);
							break;
						} catch (BufferUnderflowException e) {
							b[0] = bi.next();
						}
					yield new String(bb2);
				} else
					throw new RuntimeException();
			}
			};
		});
	}
}
