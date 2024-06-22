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
import java.util.stream.IntStream;
import java.util.stream.Stream;

class HeaderEncoder {

	boolean huffman;

	HeaderTable table;

	{
		table = new HeaderTable();
		table.dynamic = true;
		table.startIndex = HeaderTable.staticTable.startIndex + HeaderTable.staticTable.size();
	}

	public void setHuffman(boolean huffman) {
		this.huffman = huffman;
	}

	IntStream encodeHeaderList(List<Header> headers) {
		return headers.stream().flatMapToInt(this::encodeHeader);
	}

	IntStream encodeHeader(Header header) {
		var ni = header.name().equals("password");
		var b = IntStream.builder();
		var hh1 = HeaderTable.staticTable.headers(header.name());
		var hh2 = table.headers(header.name());
		var hh = Stream.concat(hh1 != null ? hh1.stream() : Stream.empty(), hh2 != null ? hh2.stream() : Stream.empty())
				.toList();
		HeaderTable.IndexAndHeader h;
		if (!hh.isEmpty()) {
			h = hh.stream().filter(x -> x.header().equals(header)).findFirst().orElse(null);
			if (h != null)
				encodeInteger(h.index(), 0x80, 7).forEach(b::add);
			else
				encodeInteger(hh.get(0).index(), 0x40, 6).forEach(b::add);
		} else {
			h = null;
			b.add(ni ? 0x10 : 0x40);
			encodeString(header.name(), b);
		}
		if (h == null) {
			encodeString(header.value(), b);
			if (!ni)
				table.add(header);
		}
		return b.build();
	}

	IntStream encodeInteger(int value, int first, int prefix) {
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

	void encodeString(String string, IntStream.Builder builder) {
		encodeInteger(string.length(), huffman ? 0x80 : 0, 7).forEach(builder::add);
		(huffman ? Huffman.encode(string) : string.chars()).forEach(builder::add);
	}
}
