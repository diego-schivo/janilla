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

	IntStream encode(HeaderField header, HeaderField.Representation literalRepresentation) {
		var ih = indexAndHeader(header);
		var r = ih != null && ih.header().equals(header) ? HeaderField.Representation.INDEXED : literalRepresentation;
		if (r == HeaderField.Representation.INDEXED)
			return Hpack.encodeInteger(ih.index(), r.prefix(), r.first());
		var b = IntStream.builder();
		if (ih != null)
			Hpack.encodeInteger(ih.index(), literalRepresentation.prefix(), literalRepresentation.first())
					.forEach(b::add);
		else {
			b.add(literalRepresentation.first());
			Hpack.encodeString(header.name(), huffman).forEach(b::add);
		}
		Hpack.encodeString(header.value(), huffman).forEach(b::add);
		if (literalRepresentation != HeaderField.Representation.NEVER_INDEXED)
			table.add(header);
		return b.build();
	}

	HeaderTable.IndexAndHeader indexAndHeader(HeaderField header) {
		var hh1 = HeaderTable.STATIC.headers(header.name());
		var hh2 = table.headers(header.name());
		var hh = Stream.concat(hh1 != null ? hh1.stream() : Stream.empty(), hh2 != null ? hh2.stream() : Stream.empty())
				.toList();
		var h = hh.stream().filter(x -> x.header().equals(header)).findFirst().orElse(null);
		if (h == null && !hh.isEmpty())
			h = hh.getFirst();
		return h;
	}
}
