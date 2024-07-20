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

import java.util.function.IntConsumer;
import java.util.stream.Stream;

import com.janilla.media.HeaderField;

public class HeaderEncoder {

	HeaderTable table = new HeaderTable();

	{
		table.setDynamic(true);
		table.setStartIndex(HeaderTable.STATIC.maxIndex() + 1);
	}

	public HeaderTable table() {
		return table;
	}

	public int encode(HeaderField header, IntConsumer bytes) {
		return encode(header, bytes, false);
	}

	public int encode(HeaderField header, IntConsumer bytes, boolean huffman) {
		return encode(header, bytes, huffman, null);
	}

	public int encode(HeaderField header, IntConsumer bytes, boolean huffman, Representation representation) {
		var ih = indexAndHeader(header);
		var r = ih != null && ih.header().equals(header) ? Representation.INDEXED
				: representation != null ? representation : Representation.WITH_INDEXING;
		if (r == Representation.INDEXED)
			return Hpack.encodeInteger(ih.index(), bytes, r.first(), r.prefix());
		int n;
		if (ih != null) {
			n = Hpack.encodeInteger(ih.index(), bytes, r.first(), r.prefix());
		} else {
			bytes.accept(Byte.toUnsignedInt(r.first()));
			n = 1 + Hpack.encodeString(header.name(), bytes, huffman);
		}
		n += Hpack.encodeString(header.value(), bytes, huffman);
		if (r != Representation.NEVER_INDEXED)
			table.add(header);
		return n;
	}

	public void encodeDynamicTableSizeUpdate(int maxSize, IntConsumer bytes) {
		Hpack.encodeInteger(maxSize, bytes, (byte) 0x20, 5);
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
