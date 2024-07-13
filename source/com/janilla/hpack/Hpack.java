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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.janilla.io.IO;
import com.janilla.media.HeaderField;
import com.janilla.util.Util;

public class Hpack {

	public static void main(String[] args) {
		integerRepresentationExamples();
		headerFieldRepresentationExamples();
		requestExamplesWithoutHuffmanCoding();
		requestExamplesWithHuffmanCoding();
		responseExamplesWithoutHuffmanCoding();
		responseExamplesWithHuffmanCoding();
	}

	public static void encodeInteger(int value, ByteBuffer buffer, byte first, int prefix) {
		var z = 1 << prefix;
		if (value < z - 1) {
			buffer.put((byte) ((Byte.toUnsignedInt(first) & (-1 ^ (z - 1))) | value));
			return;
		}
		buffer.put((byte) (Byte.toUnsignedInt(first) | (z - 1)));
		value -= (z - 1);
		var y = 1 << 7;
		while (value >= y) {
			buffer.put((byte) (y | (value & (y - 1))));
			value >>>= 7;
		}
		buffer.put((byte) value);
	}

	public static int decodeInteger(ByteBuffer buffer, int prefix) {
		var z = 1 << prefix;
		var i = Byte.toUnsignedInt(buffer.get()) & (z - 1);
		if (i < z - 1)
			return i;
		for (var m = 0;; m += 7) {
			var j = Byte.toUnsignedInt(buffer.get());
			i += (j & 0x7f) << m;
			if ((j & 0x80) == 0)
				break;
		}
		return i;
	}

	public static void encodeString(String string, ByteBuffer buffer, boolean huffman) {
		var p = buffer.position();
		if (huffman)
			Huffman.encode(string, buffer);
		else
			buffer.put(string.getBytes());
		var bb = Arrays.copyOfRange(buffer.array(), p, buffer.position());
		buffer.position(p);
		encodeInteger(bb.length, buffer, (byte) (huffman ? 0x80 : 0x00), 7);
		buffer.put(bb);
	}

	public static String decodeString(ByteBuffer buffer) {
		var h = (Byte.toUnsignedInt(buffer.get(buffer.position())) & 0x80) != 0;
//		System.out.println("h=" + h);
		var l = decodeInteger(buffer, 7);
		if (h) {
			var l0 = buffer.limit();
			buffer.limit(buffer.position() + l);
			var s = Huffman.decode(buffer);
			buffer.limit(l0);
			return s;
		}
		var cc = new char[l];
		for (var i = 0; i < l; i++)
			cc[i] = (char) Byte.toUnsignedInt(buffer.get());
		return new String(cc);
	}

	static void integerRepresentationExamples() {
		var bb = ByteBuffer.allocate(10);
		String s;

		bb.clear();
		encodeInteger(10, bb, (byte) 0x00, 5);
		bb.flip();
		s = Util.toBinaryString(IO.toIntStream(bb));
		System.out.println("s=" + s);
		assert s.equals("1010") : s;

		bb.clear();
		encodeInteger(1337, bb, (byte) 0x00, 5);
		bb.flip();
		s = Util.toBinaryString(IO.toIntStream(bb));
		System.out.println("s=" + s);
		assert s.equals("11111 10011010 1010") : s;

		bb.clear();
		encodeInteger(42, bb, (byte) 0x00, 8);
		bb.flip();
		s = Util.toBinaryString(IO.toIntStream(bb));
		System.out.println("s=" + s);
		assert s.equals("101010") : s;
	}

	static void headerFieldRepresentationExamples() {
		var bb = ByteBuffer.allocate(100);
		String s;
		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField("custom-key", "custom-header"));
		System.out.println("hh1=" + hh1);
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				400a 6375 7374 6f6d 2d6b 6579 0d63 7573
				746f 6d2d 6865 6164 6572""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 55 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":path", "/sample/path"));
		System.out.println("hh1=" + hh1);
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, false, Representation.WITHOUT_INDEXING);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("040c 2f73 616d 706c 652f 7061 7468") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField("password", "secret"));
		System.out.println("hh1=" + hh1);
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, false, Representation.NEVER_INDEXED);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				1008 7061 7373 776f 7264 0673 6563 7265
				74""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;

		e = new HeaderEncoder();
		d = new HeaderDecoder();
		hh1 = List.of(new HeaderField(":method", "GET"));
		System.out.println("hh1=" + hh1);
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("82") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 0 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void requestExamplesWithoutHuffmanCoding() {
		var bb = ByteBuffer.allocate(1000);
		String s;
		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;
		e = new HeaderEncoder();
		d = new HeaderDecoder();
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				8286 8441 0f77 7777 2e65 7861 6d70 6c65
				2e63 6f6d""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("8286 84be 5808 6e6f 2d63 6163 6865") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				8287 85bf 400a 6375 7374 6f6d 2d6b 6579
				0c63 7573 746f 6d2d 7661 6c75 65""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 164 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void requestExamplesWithHuffmanCoding() {
		var bb = ByteBuffer.allocate(1000);
		String s;
		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;
		e = new HeaderEncoder();
		d = new HeaderDecoder();
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				8286 8441 8cf1 e3c2 e5f2 3a6b a0ab 90f4
				ff""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("8286 84be 5886 a8eb 1064 9cbf") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				8287 85bf 4088 25a8 49e9 5ba9 7d7f 8925
				a849 e95b b8e8 b4bf""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 164 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void responseExamplesWithoutHuffmanCoding() {
		var bb = ByteBuffer.allocate(1000);
		String s;
		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;
		e = new HeaderEncoder();
		e.table().setMaxSize(256);
		d = new HeaderDecoder();
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				4803 3330 3258 0770 7269 7661 7465 611d
				4d6f 6e2c 2032 3120 4f63 7420 3230 3133
				2032 303a 3133 3a32 3120 474d 546e 1768
				7474 7073 3a2f 2f77 7777 2e65 7861 6d70
				6c65 2e63 6f6d""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("4803 3330 37c1 c0bf") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				88c1 611d 4d6f 6e2c 2032 3120 4f63 7420
				3230 3133 2032 303a 3133 3a32 3220 474d
				54c0 5a04 677a 6970 7738 666f 6f3d 4153
				444a 4b48 514b 425a 584f 5157 454f 5049
				5541 5851 5745 4f49 553b 206d 6178 2d61
				6765 3d33 3630 303b 2076 6572 7369 6f6e
				3d31""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 215 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}

	static void responseExamplesWithHuffmanCoding() {
		var bb = ByteBuffer.allocate(1000);
		String s;
		HeaderEncoder e;
		HeaderDecoder d;
		List<HeaderField> hh1, hh2;
		e = new HeaderEncoder();
		e.table().setMaxSize(256);
		d = new HeaderDecoder();
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				4882 6402 5885 aec3 771a 4b61 96d0 7abe
				9410 54d4 44a8 2005 9504 0b81 66e0 82a6
				2d1b ff6e 919d 29ad 1718 63c7 8f0b 97c8
				e9ae 82ae 43d3""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("4883 640e ffc1 c0bf") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
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
		bb.clear();
		for (var h : hh1)
			e.encode(h, bb, true);
		bb.flip();
		s = Util.toHexString(IO.toIntStream(bb));
		System.out.println(s);
		assert s.equals("""
				88c1 6196 d07a be94 1054 d444 a820 0595
				040b 8166 e084 a62d 1bff c05a 839b d9ab
				77ad 94e7 821d d7f2 e6c7 b335 dfdf cd5b
				3960 d5af 2708 7f36 72c1 ab27 0fb5 291f
				9587 3160 65c0 03ed 4ee5 b106 3d50 07""") : s;
		bb.position(0);
		d.headerFields().clear();
		while (bb.hasRemaining())
			d.decode(bb);
		hh2 = d.headerFields();
		System.out.println("d.table=" + d.table());
		assert d.table().size() == 215 : d.table().size();
		System.out.println("hh2=" + hh2);
		assert hh2.equals(hh1) : hh2;
	}
}
