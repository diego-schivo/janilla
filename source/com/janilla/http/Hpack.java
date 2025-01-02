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
package com.janilla.http;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import com.janilla.util.Util;

class Hpack {

	public static void main(String[] args) {
		integerRepresentationExamples();
		headerFieldRepresentationExamples();
		requestExamplesWithoutHuffmanCoding();
		requestExamplesWithHuffmanCoding();
		responseExamplesWithoutHuffmanCoding();
		responseExamplesWithHuffmanCoding();
	}

	static int encodeInteger(int value, IntConsumer bytes, byte first, int prefix) {
		var z = 1 << prefix;
		if (value < z - 1) {
			bytes.accept((Byte.toUnsignedInt(first) & (-1 ^ (z - 1))) | value);
			return 1;
		}
		bytes.accept(Byte.toUnsignedInt(first) | (z - 1));
		var n = 1;
		value -= (z - 1);
		var y = 1 << 7;
		while (value >= y) {
			bytes.accept(y | (value & (y - 1)));
			n++;
			value >>>= 7;
		}
		bytes.accept(value);
		return n + 1;
	}

	static int decodeInteger(PrimitiveIterator.OfInt bytes, int prefix) {
		var z = 1 << prefix;
		var i = bytes.nextInt() & (z - 1);
		if (i < z - 1)
			return i;
		for (var m = 0;; m += 7) {
			var j = bytes.nextInt();
			i += (j & 0x7f) << m;
			if ((j & 0x80) == 0)
				break;
		}
		return i;
	}

	static int encodeString(String string, IntConsumer bytes, boolean huffman) {
		byte[] bb;
		if (huffman) {
			var ii = IntStream.builder();
			var l = Huffman.encode(string, ii);
			var i = ii.build().iterator();
			bb = new byte[l];
			for (var j = 0; j < l; j++)
				bb[j] = (byte) i.nextInt();
		} else
			bb = string.getBytes();
		var n = encodeInteger(bb.length, bytes, (byte) (huffman ? 0x80 : 0x00), 7);
		for (var b : bb)
			bytes.accept(Byte.toUnsignedInt(b));
		return n + bb.length;
	}

	static String decodeString(PrimitiveIterator.OfInt bytes) {
		var bb = new PeekingIntIterator(bytes);
		boolean h;
		{
			var i = bb.peek();
			h = (i & 0x80) != 0;
//			System.out.println("Hpack.decodeString, h=" + h);
		}
		var l = decodeInteger(bb, 7);
		if (h) {
			var s = Huffman.decode(bb, l);
			return s;
		}
		var cc = new char[l];
		for (var i = 0; i < l; i++)
			cc[i] = (char) bb.nextInt();
		return new String(cc);
	}

	static void integerRepresentationExamples() {
		IntStream.Builder bb;
		String s;

		bb = IntStream.builder();
		encodeInteger(10, bb, (byte) 0x00, 5);
		s = Util.toBinaryString(bb.build());
		System.out.println("s=" + s);
		assert s.equals("1010") : s;

		bb = IntStream.builder();
		encodeInteger(1337, bb, (byte) 0x00, 5);
		s = Util.toBinaryString(bb.build());
		System.out.println("s=" + s);
		assert s.equals("11111 10011010 1010") : s;

		bb = IntStream.builder();
		encodeInteger(42, bb, (byte) 0x00, 8);
		s = Util.toBinaryString(bb.build());
		System.out.println("s=" + s);
		assert s.equals("101010") : s;
	}

	static void headerFieldRepresentationExamples() {
		List<HeaderField> hh1, hh2;
		HeaderEncoder e;
		HeaderDecoder d;
		IntStream.Builder bb1;
		PrimitiveIterator.OfInt bb2;
		String s;

		hh1 = List.of(new HeaderField("custom-key", "custom-header"));
		System.out.println("hh1=" + hh1);
		e = new HeaderEncoder();
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				400a 6375 7374 6f6d 2d6b 6579 0d63 7573
				746f 6d2d 6865 6164 6572""") : s;
		bb2 = Util.parseHex(s).iterator();
		d = new HeaderDecoder();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 55 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		hh1 = List.of(new HeaderField(":path", "/sample/path"));
		System.out.println("hh1=" + hh1);
		e = new HeaderEncoder();
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, false, HeaderField.Representation.WITHOUT_INDEXING);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("040c 2f73 616d 706c 652f 7061 7468") : s;
		bb2 = Util.parseHex(s).iterator();
		d = new HeaderDecoder();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField("password", "secret"));
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, false, HeaderField.Representation.NEVER_INDEXED);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				1008 7061 7373 776f 7264 0673 6563 7265
				74""") : s;
		bb2 = Util.parseHex(s).iterator();
		d = new HeaderDecoder();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":method", "GET"));
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("82") : s;
		bb2 = Util.parseHex(s).iterator();
		d = new HeaderDecoder();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void requestExamplesWithoutHuffmanCoding() {
		List<HeaderField> hh1, hh2;
		IntStream.Builder bb1;
		PrimitiveIterator.OfInt bb2;
		var e = new HeaderEncoder();
		var d = new HeaderDecoder();
		String s;
		hh1 = """
				:method: GET
				:scheme: http
				:path: /
				:authority: www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				8286 8441 0f77 7777 2e65 7861 6d70 6c65
				2e63 6f6d""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 57 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:method: GET
				:scheme: http
				:path: /
				:authority: www.example.com
				cache-control: no-cache""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("8286 84be 5808 6e6f 2d63 6163 6865") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 110 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:method: GET
				:scheme: https
				:path: /index.html
				:authority: www.example.com
				custom-key: custom-value""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				8287 85bf 400a 6375 7374 6f6d 2d6b 6579
				0c63 7573 746f 6d2d 7661 6c75 65""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 164 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void requestExamplesWithHuffmanCoding() {
		List<HeaderField> hh1, hh2;
		IntStream.Builder bb1;
		PrimitiveIterator.OfInt bb2;
		var e = new HeaderEncoder();
		var d = new HeaderDecoder();
		String s;
		hh1 = """
				:method: GET
				:scheme: http
				:path: /
				:authority: www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				8286 8441 8cf1 e3c2 e5f2 3a6b a0ab 90f4
				ff""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 57 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:method: GET
				:scheme: http
				:path: /
				:authority: www.example.com
				cache-control: no-cache""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("8286 84be 5886 a8eb 1064 9cbf") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 110 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:method: GET
				:scheme: https
				:path: /index.html
				:authority: www.example.com
				custom-key: custom-value""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				8287 85bf 4088 25a8 49e9 5ba9 7d7f 8925
				a849 e95b b8e8 b4bf""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 164 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void responseExamplesWithoutHuffmanCoding() {
		List<HeaderField> hh1, hh2;
		IntStream.Builder bb1;
		PrimitiveIterator.OfInt bb2;
		var e = new HeaderEncoder();
		var d = new HeaderDecoder();
		String s;
		e.table().setMaxSize(256);
		d.table().setMaxSize(256);
		hh1 = """
				:status: 302
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:21 GMT
				location: https://www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				4803 3330 3258 0770 7269 7661 7465 611d
				4d6f 6e2c 2032 3120 4f63 7420 3230 3133
				2032 303a 3133 3a32 3120 474d 546e 1768
				7474 7073 3a2f 2f77 7777 2e65 7861 6d70
				6c65 2e63 6f6d""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 222 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:status: 307
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:21 GMT
				location: https://www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("4803 3330 37c1 c0bf") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 222 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:status: 200
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:22 GMT
				location: https://www.example.com
				content-encoding: gzip
				set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				88c1 611d 4d6f 6e2c 2032 3120 4f63 7420
				3230 3133 2032 303a 3133 3a32 3220 474d
				54c0 5a04 677a 6970 7738 666f 6f3d 4153
				444a 4b48 514b 425a 584f 5157 454f 5049
				5541 5851 5745 4f49 553b 206d 6178 2d61
				6765 3d33 3630 303b 2076 6572 7369 6f6e
				3d31""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 215 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void responseExamplesWithHuffmanCoding() {
		List<HeaderField> hh1, hh2;
		IntStream.Builder bb1;
		PrimitiveIterator.OfInt bb2;
		var e = new HeaderEncoder();
		var d = new HeaderDecoder();
		String s;
		e.table().setMaxSize(256);
		d.table().setMaxSize(256);
		hh1 = """
				:status: 302
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:21 GMT
				location: https://www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				4882 6402 5885 aec3 771a 4b61 96d0 7abe
				9410 54d4 44a8 2005 9504 0b81 66e0 82a6
				2d1b ff6e 919d 29ad 1718 63c7 8f0b 97c8
				e9ae 82ae 43d3""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 222 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:status: 307
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:21 GMT
				location: https://www.example.com""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("4883 640e ffc1 c0bf") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 222 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
		hh1 = """
				:status: 200
				cache-control: private
				date: Mon, 21 Oct 2013 20:13:22 GMT
				location: https://www.example.com
				content-encoding: gzip
				set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1""".lines().map(x -> {
			var i = x.indexOf(':', 1);
			var n = x.substring(0, i).trim();
			var v = x.substring(i + 1).trim();
			return new HeaderField(n, v);
		}).toList();
		System.out.println("hh1=" + hh1);
		bb1 = IntStream.builder();
		for (var h : hh1)
			e.encode(h, bb1, true);
		s = Util.toHexString(bb1.build());
		System.out.println(s);
		assert s.equals("""
				88c1 6196 d07a be94 1054 d444 a820 0595
				040b 8166 e084 a62d 1bff c05a 839b d9ab
				77ad 94e7 821d d7f2 e6c7 b335 dfdf cd5b
				3960 d5af 2708 7f36 72c1 ab27 0fb5 291f
				9587 3160 65c0 03ed 4ee5 b106 3d50 07""") : s;
		bb2 = Util.parseHex(s).iterator();
		d.headerFields().clear();
		while (bb2.hasNext())
			d.decode(bb2);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 215 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}
}
