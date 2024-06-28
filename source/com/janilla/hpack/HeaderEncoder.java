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

import java.util.stream.Stream;

import com.janilla.http2.BitsWriter;

public class HeaderEncoder {

	BitsWriter bits;

	HeaderTable table;

	public HeaderEncoder(BitsWriter bits) {
		this.bits = bits;
		table = new HeaderTable();
		table.setDynamic(true);
		table.setStartIndex(HeaderTable.STATIC.maxIndex() + 1);
	}

	public HeaderTable table() {
		return table;
	}

	public void encode(HeaderField header) {
		encode(header, false);
	}

	public void encode(HeaderField header, boolean huffman) {
		encode(header, huffman, null);
	}

	public void encode(HeaderField header, boolean huffman, HeaderField.Representation representation) {
		var ih = indexAndHeader(header);
		var r = ih != null && ih.header().equals(header) ? HeaderField.Representation.INDEXED
				: representation != null ? representation : HeaderField.Representation.WITH_INDEXING;
		if (r == HeaderField.Representation.INDEXED) {
			bits.accept(r.first() >>> r.prefix(), 8 - r.prefix());
			Hpack.encodeInteger(ih.index(), r.prefix(), bits);
			return;
		}
		if (ih != null) {
			bits.accept(r.first() >>> r.prefix(), 8 - r.prefix());
			Hpack.encodeInteger(ih.index(), r.prefix(), bits);
		} else {
			bits.accept(r.first());
			Hpack.encodeString(header.name(), huffman, bits);
		}
		Hpack.encodeString(header.value(), huffman, bits);
		if (r != HeaderField.Representation.NEVER_INDEXED)
			table.add(header);
	}

	public void encodeDynamicTableSizeUpdate(int maxSize) {
		bits.accept(0x01, 3);
		Hpack.encodeInteger(maxSize, 5, bits);
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
