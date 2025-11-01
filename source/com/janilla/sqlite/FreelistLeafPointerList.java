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
import java.util.Objects;
import java.util.RandomAccess;

public class FreelistLeafPointerList extends AbstractList<Long> implements RandomAccess {

	protected final FreelistTrunkPage trunkPage;

	public FreelistLeafPointerList(FreelistTrunkPage trunkPage) {
		this.trunkPage = trunkPage;
	}

	@Override
	public void add(int index, Long element) {
		var s = size();
		if (8 + (s + 1) * Integer.BYTES > trunkPage.buffer().limit())
			throw new IllegalStateException();
		Objects.checkIndex(index, s + 1);
		var i = 8 + index * Integer.BYTES;
		if (index != s)
			System.arraycopy(trunkPage.buffer().array(), i, trunkPage.buffer().array(), i + Integer.BYTES,
					(s - index) * Integer.BYTES);
		trunkPage.buffer().putInt(i, element.intValue());
		setSize(s + 1);
	}

	@Override
	public void clear() {
		setSize(0);
	}

	@Override
	public Long get(int index) {
		Objects.checkIndex(index, size());
		return Integer.toUnsignedLong(trunkPage.buffer().getInt(8 + index * Integer.BYTES));
	}

	@Override
	public int size() {
		return trunkPage.buffer().getInt(4);
	}

	@Override
	public Long remove(int index) {
		var x = get(index);
		var i = 8 + index * Integer.BYTES;
		var s = size();
		if (index != s - 1)
			System.arraycopy(trunkPage.buffer().array(), i + Integer.BYTES, trunkPage.buffer().array(), i,
					(s - 1 - index) * Integer.BYTES);
		setSize(s - 1);
		return x;
	}

	protected void setSize(int size) {
		trunkPage.buffer().putInt(4, size);
	}
}
