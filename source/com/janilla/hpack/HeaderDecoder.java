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

import java.util.PrimitiveIterator;
import java.util.stream.Stream;

class HeaderDecoder {

	HeaderTable table;

	{
		table = new HeaderTable();
		table.setDynamic(true);
		table.setStartIndex(HeaderTable.STATIC.maxIndex() + 1);
	}

	public HeaderTable table() {
		return table;
	}

	Stream<HeaderField> decode(PrimitiveIterator.OfInt bytes) {
		var bb = bytes instanceof IntIterator x ? x : new IntIterator(bytes);
		return Stream.generate(() -> decodeNext(bb)).takeWhile(x -> x != null);
	}

	HeaderField decodeNext(PrimitiveIterator.OfInt bytes) {
		if (!bytes.hasNext())
			return null;
		var bb = bytes instanceof IntIterator x ? x : new IntIterator(bytes);
		var b = bb.nextInt(true);
		if ((b & 0x80) != 0) {
			var i = Hpack.decodeInteger(bb, 7);
			var h = (i <= HeaderTable.STATIC.maxIndex() ? HeaderTable.STATIC : table).header(i);
			return h;
		} else if ((b & 0x40) != 0) {
			if ((b & 0x3f) != 0) {
				var i = Hpack.decodeInteger(bb, 6);
				var h0 = HeaderTable.STATIC.header(i);
				var v = Hpack.decodeString(bb);
				var h = new HeaderField(h0.name(), v);
				table.add(h);
				return h;
			} else {
				bb.nextInt();
				var n = Hpack.decodeString(bb);
				var v = Hpack.decodeString(bb);
				var h = new HeaderField(n, v);
				table.add(h);
				return h;
			}
		} else if ((b & 0x10) != 0) {
			bb.nextInt();
			var n = Hpack.decodeString(bb);
			var v = Hpack.decodeString(bb);
			var h = new HeaderField(n, v);
			return h;
		} else if ((b & 0xf0) == 0) {
			if ((b & 0x0f) != 0) {
				var i = Hpack.decodeInteger(bb, 4);
				var h0 = HeaderTable.STATIC.header(i);
				var v = Hpack.decodeString(bb);
				var h = new HeaderField(h0.name(), v);
				return h;
			} else {
				bb.nextInt();
				var n = Hpack.decodeString(bb);
				var v = Hpack.decodeString(bb);
				var h = new HeaderField(n, v);
				return h;
			}
		}
		throw new RuntimeException();
	}
}
