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

import java.util.ArrayList;
import java.util.List;

import com.janilla.http2.BitsReader;

public class HeaderDecoder {

	HeaderTable table;

	List<Integer> dynamicTableMaxSizes = new ArrayList<>();

	List<HeaderField> headerFields = new ArrayList<>();

	{
		table = new HeaderTable();
		table.setDynamic(true);
		table.setStartIndex(HeaderTable.STATIC.maxIndex() + 1);
	}

	public HeaderTable table() {
		return table;
	}

	public List<Integer> dynamicTableMaxSizes() {
		return dynamicTableMaxSizes;
	}

	public List<HeaderField> headerFields() {
		return headerFields;
	}

	public void decode(BitsReader bits) {
		if (bits.nextInt(1) == 0x01) {
			System.out.println(HeaderField.Representation.INDEXED);
			var i = Hpack.decodeInteger(bits, 7);
			var h = (i <= HeaderTable.STATIC.maxIndex() ? HeaderTable.STATIC : table).header(i);
			headerFields.add(h);
			return;
		}
		if (bits.nextInt(1) == 0x01) {
			System.out.println(HeaderField.Representation.WITH_INDEXING);
			var i = Hpack.decodeInteger(bits, 6);
			if (i != 0) {
				var h0 = (i <= HeaderTable.STATIC.maxIndex() ? HeaderTable.STATIC : table).header(i);
				var v = Hpack.decodeString(bits);
				var h = new HeaderField(h0.name(), v);
				table.add(h);
				headerFields.add(h);
				return;
			}
			var n = Hpack.decodeString(bits);
			var v = Hpack.decodeString(bits);
			var h = new HeaderField(n, v);
			table.add(h);
			headerFields.add(h);
			return;
		}
		if (bits.nextInt(1) == 0x01) {
			var s = Hpack.decodeInteger(bits, 5);
			table.setMaxSize(s);
			dynamicTableMaxSizes.add(s);
			return;
		}
		if (bits.nextInt(1) == 0x01) {
			System.out.println(HeaderField.Representation.NEVER_INDEXED);
			var i = Hpack.decodeInteger(bits, 4);
			if (i != 0) {
				var h0 = (i <= HeaderTable.STATIC.maxIndex() ? HeaderTable.STATIC : table).header(i);
				var v = Hpack.decodeString(bits);
				var h = new HeaderField(h0.name(), v);
				headerFields.add(h);
				return;
			}
			var n = Hpack.decodeString(bits);
			var v = Hpack.decodeString(bits);
			var h = new HeaderField(n, v);
			headerFields.add(h);
			return;
		}
		System.out.println(HeaderField.Representation.WITHOUT_INDEXING);
		var i = Hpack.decodeInteger(bits, 4);
		if (i != 0) {
			var h0 = (i <= HeaderTable.STATIC.maxIndex() ? HeaderTable.STATIC : table).header(i);
			var v = Hpack.decodeString(bits);
			var h = new HeaderField(h0.name(), v);
			headerFields.add(h);
			return;
		}
		var n = Hpack.decodeString(bits);
		var v = Hpack.decodeString(bits);
		var h = new HeaderField(n, v);
		headerFields.add(h);
		return;
	}
}
