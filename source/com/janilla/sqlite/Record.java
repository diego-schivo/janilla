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
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Record {

//	public static void main(String[] args) {
//		var e1 = RecordColumn.of(70l);
//		var e2 = RecordColumn.of(1l);
//		IO.println(e1.compareTo(e2));
//	}

	public static byte[] toBytes(RecordColumn... columns) {
		var s = Arrays.stream(columns).mapToInt(x -> Varint.size(x.type())).sum();
		var s1 = Varint.size(s);
		var s2 = Varint.size(s1 + s);
		var bb = new byte[(s2 != s1 ? Varint.size(s2 + s) : s2) + s
				+ Arrays.stream(columns).mapToInt(x -> x.content().length).sum()];
		var b = ByteBuffer.wrap(bb);
		Varint.put(b, s2 + s);
		for (var x : columns)
			Varint.put(b, x.type());
		for (var x : columns)
			b.put(x.content());
		return bb;
	}

	public static Stream<RecordColumn> fromBytes(Stream<ByteBuffer> buffers) {
		var bi = buffers.iterator();
		class A {
			ByteBuffer b = bi.next();
		}
		var a = new A();
//		IO.println("a.b=" + a.b.remaining());
		int[] tt;
		{
			var l = Varint.get(a.b);
			var ii = IntStream.builder();
			while (a.b.position() < l)
				ii.add((int) Varint.get(a.b));
			tt = ii.build().toArray();
		}
//		IO.println("Record.fromBytes, tt=" + Arrays.toString(tt));
		return Arrays.stream(tt).mapToObj(t -> {
			byte[] c;
			switch (t) {
			case 0:
				c = new byte[0];
				break;
			case 1, 2, 3, 4, 5, 6:
				var n = (new int[] { 8, 16, 24, 32, 48, 64 })[t - 1];
				c = new byte[n / 8];
				a.b.get(c);
//					IO.println("n=" + n + ", l=" + l);
				break;
			case 7:
				c = new byte[Double.BYTES];
				a.b.get(c);
				break;
			case 8, 9, 12, 13:
				c = new byte[0];
				break;
			default:
				if (t >= 14 && t % 2 == 0) {
					c = new byte[(t - 12) / 2];
//						IO.println("bb=" + bb.length);
					for (;;)
						try {
//								IO.println("a.b=" + a.b.remaining());
							a.b.get(c);
							break;
						} catch (BufferUnderflowException e) {
							a.b = bi.next();
						}
					break;
				} else if (t >= 15 && t % 2 == 1) {
					c = new byte[(t - 13) / 2];
//						IO.println("bb=" + bb.length);
					for (;;)
						try {
//								IO.println("a.b=" + a.b.remaining());
							a.b.get(c);
							break;
						} catch (BufferUnderflowException e) {
							a.b = bi.next();
						}
					break;
				} else
					throw new RuntimeException();
			}
			return new RecordColumn(t, c);
		});
	}

	public static int compare(Iterator<RecordColumn> a, Iterator<RecordColumn> b) {
		while (a.hasNext() && b.hasNext()) {
			var d = a.next().compareTo(b.next());
			if (d != 0)
				return d;
		}
		return 0;
	}
}
