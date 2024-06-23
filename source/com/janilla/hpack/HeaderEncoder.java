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

	HeaderTable table;

	boolean huffman;

	{
		table = new HeaderTable();
		table.setDynamic(true);
		table.setStartIndex(HeaderTable.STATIC.maxIndex() + 1);
	}

	public HeaderTable table() {
		return table;
	}

	public void setHuffman(boolean huffman) {
		this.huffman = huffman;
	}

	IntStream encode(List<HeaderField> headers, HeaderField.Representation representation) {
		return headers.stream().flatMapToInt(x -> encode(x, representation));
	}

	IntStream encode(HeaderField header, HeaderField.Representation representation) {
		HeaderTable.IndexAndHeader h;
		{
			var hh1 = HeaderTable.STATIC.headers(header.name());
			var hh2 = table.headers(header.name());
			var hh = Stream
					.concat(hh1 != null ? hh1.stream() : Stream.empty(), hh2 != null ? hh2.stream() : Stream.empty())
					.toList();
			h = hh.stream().filter(x -> x.header().equals(header)).findFirst().orElse(null);
			if (h == null && !hh.isEmpty())
				h = hh.getFirst();
		}
		if (h != null && h.header().equals(header))
			return Hpack.encodeInteger(h.index(), 7, 0x80);
		int p, f;
		switch (representation) {
		case WITH_INDEXING:
			p = 6;
			f = 0x40;
			break;
		case WITHOUT_INDEXING:
			p = 4;
			f = 0x00;
			break;
		case NEVER_INDEXED:
			p = 4;
			f = 0x10;
			break;
		default:
			throw new RuntimeException();
		}
		var b = IntStream.builder();
		if (h != null)
			Hpack.encodeInteger(h.index(), p, f).forEach(b::add);
		else {
			b.add(f);
			Hpack.encodeString(header.name(), huffman).forEach(b::add);
		}
		if (h == null || !h.header().equals(header)) {
			Hpack.encodeString(header.value(), huffman).forEach(b::add);
			switch (representation) {
			case NEVER_INDEXED:
				break;
			default:
				table.add(header);
				break;
			}
		}
		return b.build();
	}
}
