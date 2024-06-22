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

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Hpack {

	public static void main(String[] args) {
		var e = new HeaderEncoder();
		e.setHuffman(true);

//		System.out.println(toBinaryString(e.encodeInteger(10, 0, 5)));
//		System.out.println(toBinaryString(e.encodeInteger(1337, 0, 5)));
//		System.out.println(toBinaryString(e.encodeInteger(42, 0, 8)));

		Header h;
		String s;
		HeaderDecoder d;

//		h = new Header("custom-key", "custom-header");
//		System.out.println(h);
//		s = toHexString(e.encodeHeader(h));
//		System.out.println(s);
//		d = new Decoder();
//		d.decodeHeader(parseHex(s).iterator());
//		System.out.println("dynamicTable=" + d.dynamicTable);
//		System.out.println("headers=" + d.headers);
//
//		h = new Header(":path", "/sample/path");
//		System.out.println(h);
//		s = toHexString(e.encodeHeader(h));
//		System.out.println(s);
//		d = new Decoder();
//		d.decodeHeader(parseHex(s).iterator());
//		System.out.println("dynamicTable=" + d.dynamicTable);
//		System.out.println("headers=" + d.headers);
//
//		h = new Header("password", "secret");
//		System.out.println(h);
//		s = toHexString(e.encodeHeader(h));
//		System.out.println(s);
//		d = new Decoder();
//		d.decodeHeader(parseHex(s).iterator());
//		System.out.println("dynamicTable=" + d.dynamicTable);
//		System.out.println("headers=" + d.headers);
//
//		h = new Header(":method", "GET");
//		System.out.println(h);
//		s = toHexString(e.encodeHeader(h));
//		System.out.println(s);
//		d = new Decoder();
//		d.decodeHeader(parseHex(s).iterator());
//		System.out.println("dynamicTable=" + d.dynamicTable);
//		System.out.println("headers=" + d.headers);

		List<Header> hh;

		hh = List.of(new Header(":method", "GET"), new Header(":scheme", "http"), new Header(":path", "/"),
				new Header(":authority", "www.example.com"));
		System.out.println(hh);
		s = toHexString(e.encodeHeaderList(hh));
		System.out.println(s);
		d = new HeaderDecoder();
		hh = d.decodeHeaderList(parseHex(s).iterator()).toList();
		System.out.println("dynamicTable=" + d.table);
		System.out.println("headers=" + hh);

		hh = List.of(new Header(":method", "GET"), new Header(":scheme", "http"), new Header(":path", "/"),
				new Header(":authority", "www.example.com"), new Header("cache-control", "no-cache"));
		System.out.println(hh);
		s = toHexString(e.encodeHeaderList(hh));
		System.out.println(s);
		hh = d.decodeHeaderList(parseHex(s).iterator()).toList();
		System.out.println("dynamicTable=" + d.table);
		System.out.println("headers=" + hh);

		hh = List.of(new Header(":method", "GET"), new Header(":scheme", "https"), new Header(":path", "/index.html"),
				new Header(":authority", "www.example.com"), new Header("custom-key", "custom-value"));
		System.out.println(hh);
		s = toHexString(e.encodeHeaderList(hh));
		System.out.println(s);
		hh = d.decodeHeaderList(parseHex(s).iterator()).toList();
		System.out.println("dynamicTable=" + d.table);
		System.out.println("headers=" + hh);
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
