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
package com.janilla.hpack;

import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Hpack {

	public static void main(String[] args) {
		String s;

		s = toBinaryString(encodeInteger(10, 5));
		System.out.println("s=" + s);
		assert s.equals("1010") : s;

		s = toBinaryString(encodeInteger(1337, 5));
		System.out.println("s=" + s);
		assert s.equals("11111 10011010 1010") : s;

		s = toBinaryString(encodeInteger(42));
		System.out.println("s=" + s);
		assert s.equals("101010") : s;

		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField("custom-key", "custom-header"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.WITH_INDEXING));
		System.out.println(s);
		assert s.equals("""
				400a 6375 7374 6f6d 2d6b 6579 0d63 7573
				746f 6d2d 6865 6164 6572""") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 55 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":path", "/sample/path"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.WITHOUT_INDEXING));
		System.out.println(s);
		assert s.equals("040c 2f73 616d 706c 652f 7061 7468") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField("password", "secret"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.NEVER_INDEXED));
		System.out.println(s);
		assert s.equals("""
				1008 7061 7373 776f 7264 0673 6563 7265
				74""") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":method", "GET"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, null));
		System.out.println(s);
		assert s.equals("82") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":method", "GET"), new HeaderField(":scheme", "http"),
				new HeaderField(":path", "/"), new HeaderField(":authority", "www.example.com"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.WITH_INDEXING));
		System.out.println(s);
		assert s.equals("""
				8286 8441 0f77 7777 2e65 7861 6d70 6c65
				2e63 6f6d""") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 57 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = List.of(new HeaderField(":method", "GET"), new HeaderField(":scheme", "http"),
				new HeaderField(":path", "/"), new HeaderField(":authority", "www.example.com"),
				new HeaderField("cache-control", "no-cache"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.WITH_INDEXING));
		System.out.println(s);
		assert s.equals("8286 84be 5808 6e6f 2d63 6163 6865") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 110 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = List.of(new HeaderField(":method", "GET"), new HeaderField(":scheme", "https"),
				new HeaderField(":path", "/index.html"), new HeaderField(":authority", "www.example.com"),
				new HeaderField("custom-key", "custom-value"));
		System.out.println("hh1=" + hh1);
		s = toHexString(e.encode(hh1, HeaderField.Representation.WITH_INDEXING));
		System.out.println(s);
		assert s.equals("""
				8287 85bf 400a 6375 7374 6f6d 2d6b 6579
				0c63 7573 746f 6d2d 7661 6c75 65""") : s;
		hh2 = d.decode(parseHex(s).iterator()).toList();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 164 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static IntStream encodeInteger(int value) {
		return encodeInteger(value, 8);
	}

	static IntStream encodeInteger(int value, int prefix) {
		return encodeInteger(value, prefix, 0);
	}

	static IntStream encodeInteger(int value, int prefix, int first) {
		var z = 1 << prefix;
		first &= -1 ^ (z - 1);
		if (value < z - 1)
			return IntStream.of(first | (value & (z - 1)));
		var b = IntStream.builder();
		b.add(first | (z - 1));
		value -= (z - 1);
		var y = 1 << 7;
		while (value >= y) {
			b.add(y | (value & (y - 1)));
			value >>>= 7;
		}
		b.add(value);
		return b.build();
	}

	static int decodeInteger(PrimitiveIterator.OfInt bytes, int prefix) {
		var z = 1 << prefix;
		var b = bytes.nextInt();
		var i = b & (z - 1);
		if (i < z - 1)
			return i;
		var m = 0;
		do {
			b = bytes.nextInt();
			i += (b & 0x7f) << m;
			m += 7;
		} while ((b & 0x80) != 0);
		return i;
	}

	static IntStream encodeString(String string, boolean huffman) {
		var ii = (huffman ? Huffman.encode(string) : string.chars()).toArray();
		return IntStream.concat(encodeInteger(ii.length, 7, huffman ? 0x80 : 0), Arrays.stream(ii));
	}

	static String decodeString(PrimitiveIterator.OfInt bytes) {
		var bb = bytes instanceof IntIterator x ? x : new IntIterator(bytes);
		var b = bb.nextInt(true);
		var h = (b & 0x80) != 0;
		var l = decodeInteger(bb, 7);
		if (h)
			return Huffman.decode(bb, l);
		var cc = new char[l];
		IntStream.range(0, l).forEach(x -> cc[x] = (char) bb.nextInt());
		return new String(cc);
	}

	static String toBinaryString(IntStream integers) {
		return integers.mapToObj(Integer::toBinaryString).collect(Collectors.joining(" "));
	}

	static String toHexString(IntStream integers) {
		return integers.mapToObj(x -> {
			var s = Integer.toHexString(x);
			return s.length() == 1 ? "0" + s : s;
		}).collect(Collector.of(StringBuilder::new, (b, x) -> {
			if (b.length() % 5 == 4)
				b.append(b.length() % 40 == 39 ? '\n' : ' ');
			b.append(x);
		}, (b1, b2) -> b1, StringBuilder::toString));
	}

	static IntStream parseHex(String string) {
		var e = (string.length() - (string.length() / 5)) / 2;
		return IntStream.range(0, e).map(x -> {
			var i = x * 2;
			i += i / 4;
			return Integer.parseInt(string.substring(i, i + 2), 16);
		});
	}
}
