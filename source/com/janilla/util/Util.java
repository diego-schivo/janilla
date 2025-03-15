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
package com.janilla.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.io.IO;

public interface Util {

	static Class<?> getClass(Path path) {
		var cn = !Files.isDirectory(path) ? path.toString().replace(File.separatorChar, '/') : null;
		cn = cn != null && cn.endsWith(".class") ? cn.substring(0, cn.length() - ".class".length()) : null;
		cn = cn != null ? cn.replace('/', '.') : null;
		try {
			return cn != null ? Class.forName(cn) : null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	static Stream<Class<?>> getPackageClasses(String package1) {
		var b = Stream.<Class<?>>builder();
		IO.acceptPackageFiles(package1, f -> {
//			System.out.println("Util.getPackageClasses, f = " + f);
			var c = Util.getClass(f);
			if (c != null)
				b.add(c);
		});
		return b.build();
	}

	static String capitalizeFirstChar(String string) {
		if (string == null || string.length() == 0)
			return string;
		return string.substring(0, 1).toUpperCase() + string.substring(1);
	}

	static String uncapitalizeFirstChar(String string) {
		if (string == null || string.length() == 0)
			return string;
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}

	static boolean startsWithIgnoreCase(String string, String prefix) {
		return string == prefix || (prefix != null && prefix.length() <= string.length()
				&& string.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	static String[] splitCamelCase(String string) {
		class A implements IntConsumer {

			StringBuilder s = new StringBuilder();

			Stream.Builder<String> b = Stream.builder();

			boolean u;

			@Override
			public void accept(int value) {
				var c = (char) value;
				var l = !u;
				u = Character.isUpperCase(c);
				if (u && s.length() > 1 && l) {
					b.add(s.toString());
					s.setLength(0);
				}
				s.append(c);
			}

			String[] build() {
				if (!s.isEmpty()) {
					b.add(s.toString());
					s.setLength(0);
				}
				return b.build().toArray(String[]::new);
			}
		}
		var a = new A();
		string.chars().forEach(a);
		return a.build();
	}

	static int[] findIndexes(byte[] bytes, byte[] search) {
		return findIndexes(bytes, search, -1);
	}

	static int[] findIndexes(byte[] bytes, byte[] search, int limit) {
		if (limit == 0)
			return new int[0];
		var w = search;
		var t = new int[w.length + 1];
		{
			var p = 1;
			var c = 0;
			t[0] = -1;
			while (p < w.length) {
				if (w[p] == w[c])
					t[p] = t[c];
				else {
					t[p] = c;
					while (c >= 0 && w[p] != w[c])
						c = t[c];
				}
				p++;
				c++;
			}
			t[p] = c;
//			System.out.println(Arrays.toString(t));
		}
		var s = bytes;
		var j = 0;
		var k = 0;
		var p = IntStream.builder();
		var n = 0;
		while (j < s.length) {
			if (w[k] == s[j]) {
				j++;
				k++;
				if (k == w.length) {
					p.add(j - k);
					n++;
					if (limit > 0 && n == limit)
						break;
					k = t[k];
				}
			} else {
				k = t[k];
				if (k < 0) {
					j++;
					k++;
				}
			}
		}
		return p.build().toArray();
	}

	public static String toBinaryString(IntStream integers) {
		return integers.mapToObj(Integer::toBinaryString).collect(Collectors.joining(" "));
	}

	public static String toHexString(IntStream integers) {
		return integers.mapToObj(x -> {
			var s = Integer.toHexString(x);
			return s.length() == 1 ? "0" + s : s;
		}).collect(Collector.of(StringBuilder::new, (b, x) -> {
			if (b.length() % 5 == 4)
				b.append(b.length() % 40 == 39 ? '\n' : ' ');
			b.append(x);
		}, (b1, _) -> b1, StringBuilder::toString));
	}

	public static IntStream toIntStream(byte[] bytes) {
		return IntStream.range(0, bytes.length).map(x -> Byte.toUnsignedInt(bytes[x]));
	}

	public static IntStream parseHex(String string) {
		var e = (string.length() - (string.length() / 5)) / 2;
		return IntStream.range(0, e).map(x -> {
			var i = x * 2;
			i += i / 4;
			return Integer.parseInt(string.substring(i, i + 2), 16);
		});
	}

//	public static PrimitiveIterator.OfInt concat(int i, PrimitiveIterator.OfInt ii) {
//		return new PrimitiveIterator.OfInt() {
//			
//			boolean x;
//
//			@Override
//			public boolean hasNext() {
//				return !x || ii.hasNext();
//			}
//
//			@Override
//			public int nextInt() {
//				if (!x) {
//					x = true;
//					return i;
//				}
//				return ii.nextInt();
//			}
//		};
//	}
}
