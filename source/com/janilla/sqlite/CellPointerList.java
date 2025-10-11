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
package com.janilla.sqlite;

import java.util.AbstractList;
import java.util.RandomAccess;

public class CellPointerList<C extends Cell> extends AbstractList<Integer> implements RandomAccess {

	protected final BTreePage<C> page;

	public CellPointerList(BTreePage<C> page) {
		this.page = page;
	}

	@Override
	public int size() {
		return page.getCellCount();
	}

	@Override
	public Integer get(int index) {
		return Short.toUnsignedInt(page.buffer.getShort(p(index)));
	}

	@Override
	public void add(int index, Integer element) {
		var p = p(index);
		var m = size() - index;
		if (m > 0)
			System.arraycopy(page.buffer.array(), p, page.buffer.array(), p + Short.BYTES, m * Short.BYTES);
		page.buffer.putShort(p, element.shortValue());
	}

	@Override
	public Integer remove(int index) {
		var p = p(index);
		var x = Short.toUnsignedInt(page.buffer.getShort(p));
		var m = size() - index - 1;
		if (m > 0)
			System.arraycopy(page.buffer.array(), p + Short.BYTES, page.buffer.array(), p, m * Short.BYTES);
		return x;
	}

	@Override
	public Integer set(int index, Integer element) {
		var p = p(index);
		var x = Short.toUnsignedInt(page.buffer.getShort(p));
		page.buffer.putShort(p, element.shortValue());
		return x;
	}

	public int p(int index) {
		var t = page.buffer.get(page.buffer.position());
//			IO.println("t=" + t + " (" + buffer.position() + ")");
		return page.buffer.position() + switch (t) {
		case 0x02, 0x05 -> 12;
		case 0x0a, 0x0d -> 8;
		default -> throw new RuntimeException();
		} + index * Short.BYTES;
	}
}
