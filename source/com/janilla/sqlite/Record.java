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
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Record {

	public static byte[] toBytes(Element... values) {
		var s = Arrays.stream(values).mapToInt(x -> Varint.size(x.type)).sum();
		var s1 = Varint.size(s);
		var s2 = Varint.size(s1 + s);
		var bb = new byte[(s2 != s1 ? Varint.size(s2 + s) : s2) + s
				+ Arrays.stream(values).mapToInt(x -> x.content.length).sum()];
		var b = ByteBuffer.wrap(bb);
		Varint.put(b, s2 + s);
		for (var x : values)
			Varint.put(b, x.type);
		for (var x : values)
			b.put(x.content);
		return bb;
	}

	public static Stream<Element> fromBytes(Stream<ByteBuffer> buffers) {
		var bi = buffers.iterator();
		class A {
			ByteBuffer b;
		}
		var a = new A();
		a.b = bi.next();
//		IO.println("a.b=" + a.b.remaining());
		int[] tt;
		{
			var l = Varint.get(a.b);
			var ii = IntStream.builder();
			while (a.b.position() < l)
				ii.add((int) Varint.get(a.b));
			tt = ii.build().toArray();
		}
//		IO.println("tt=" + Arrays.toString(tt));
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
			return new Element(t, c);
		});
	}

	public static int compare(Iterator<Element> a, Iterator<Element> b) {
		while (a.hasNext() && b.hasNext()) {
			var d = a.next().compareTo(b.next());
			if (d != 0)
				return d;
		}
		return 0;
	}

	public record Element(int type, byte[] content) implements Comparable<Element> {

		public static Element of(Object v) {
			if (v == null)
				return new Element(0, new byte[0]);
			else if (v instanceof Long x) {
				long l = x;
				switch (l) {
				case 0l:
					return new Element(8, new byte[0]);
				case 1l:
					return new Element(9, new byte[0]);
				default:
					var ni = 0;
					var nn = new int[] { 8, 16, 24, 32, 48, 64 };
					for (; ni < nn.length; ni++) {
						var m = (1l << nn[ni]) - 1;
						if ((l & m) == l)
							break;
					}
					var b = ByteBuffer.allocate(Long.BYTES);
					b.putLong(l);
					return new Element(1 + ni, Arrays.copyOfRange(b.array(), Long.BYTES - nn[ni] / 8, Long.BYTES));
				}
			} else if (v instanceof Double x) {
				var b = ByteBuffer.allocate(Double.BYTES);
				b.putDouble(x);
				return new Element(7, b.array());
			} else if (v instanceof String s) {
				var bb = s.getBytes();
				return new Element(bb.length * 2 + 13, bb);
			} else
				throw new RuntimeException(Objects.toString(v) + " (" + v.getClass() + ")");
		}

		public Object toObject() {
			switch (type) {
			case 0:
				return null;
			case 1, 2, 3, 4, 5:
				var b = ByteBuffer.allocate(Long.BYTES);
				b.put(Long.BYTES - content.length, content);
				return b.getLong();
			case 6:
				return ByteBuffer.wrap(content).getLong();
			case 7:
				return ByteBuffer.wrap(content).getDouble();
			case 8:
				return 0l;
			case 9:
				return 1l;
			default:
				if (type >= 12 && type % 2 == 0)
					return content;
				else if (type >= 13 && type % 2 == 1)
					return content.length != 0 ? new String(content) : "";
				else
					throw new RuntimeException();
			}
		}

		@Override
		public int compareTo(Element v) {
			switch (type) {
			case 0:
				return v.type == 0 ? 0 : -1;

			case 1, 2, 3, 4, 5, 6:
				switch (v.type) {
				case 0:
					return 1;
				case 1, 2, 3, 4, 5, 6:
					var d = Integer.compare(type, v.type);
					return d == 0 ? Arrays.compare(content, v.content) : d;
				default:
					return -1;
				}

			default:
				return Arrays.compare(content, v.content);
			}
		}
	}
}
