/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.backend.sqlite;

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
		return Short.toUnsignedInt(page.buffer().getShort(offset(index)));
	}

	@Override
	public void add(int index, Integer element) {
		var o = offset(index);
		var m = size() - index;
		if (m > 0)
			System.arraycopy(page.buffer().array(), o, page.buffer().array(), o + Short.BYTES, m * Short.BYTES);
		page.buffer().putShort(o, element.shortValue());
	}

	@Override
	public Integer remove(int index) {
		var o = offset(index);
		var x = Short.toUnsignedInt(page.buffer().getShort(o));
		var m = size() - index - 1;
		if (m > 0)
			System.arraycopy(page.buffer().array(), o + Short.BYTES, page.buffer().array(), o, m * Short.BYTES);
		return x;
	}

	@Override
	public Integer set(int index, Integer element) {
		var o = offset(index);
		var x = Short.toUnsignedInt(page.buffer().getShort(o));
		page.buffer().putShort(o, element.shortValue());
		return x;
	}

	public int offset(int index) {
		return page.buffer().position() + (page instanceof InteriorPage ? 12 : 8) + index * Short.BYTES;
	}
}
