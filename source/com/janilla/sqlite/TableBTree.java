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

import java.util.Arrays;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TableBTree extends BTree<TableLeafPage, TableLeafCell> {

	public TableBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public long insert(LongFunction<Object[]> values) {
		var n = rootNumber;
		var b = database.allocatePage();
		var p = (TableLeafPage) database.readBTreePage(n, b);
		var k = (p.getCellCount() != 0 ? p.getCells().getLast().key() : 0) + 1;
		var vv = values.apply(k);
//		IO.println("TableBTree.insert, k=" + k + ", vv=" + Arrays.toString(vv));
		var l = (long) vv[0];
		if (l >= k)
			k = l;
		else
			throw new RuntimeException();
		var bb = Record.toBytes(Arrays.stream(vv).skip(1).map(Record.Element::of).toArray(Record.Element[]::new));
		var is = database.initialSize(bb.length, false);
		var c = is == bb.length ? new TableLeafCell.New(bb.length, k, bb, 0)
				: new TableLeafCell.New(bb.length, k, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		if (p.getCellCount() == 0)
			p.setCellContentAreaStart(database.usableSize());
		p.getCells().add(c);
		database.write(n, b);
		database.updateHeader();
		return c.key();
	}

	@Override
	public boolean insert(Object[] key, Object[] data) {
		throw new RuntimeException();
	}

	@Override
	public Stream<Object[]> select(Object... keys) {
		throw new RuntimeException();
	}

	@Override
	public void delete(Object... keys) {
		throw new RuntimeException();
	}

	public LongStream keys() {
		return cells().mapToLong(x -> ((TableLeafCell) x).key());
	}

	@Override
	public long count(Object... keys) {
		if (keys.length == 0)
			return count(false);
		throw new RuntimeException();
	}

	@Override
	public Stream<? extends Cell> cells() {
		return cells(false);
	}

	@Override
	public Object[] row(Cell cell) {
		var oo = super.row(cell);
		var oo2 = new Object[oo.length + 1];
		oo2[0] = ((TableLeafCell) cell).key();
		System.arraycopy(oo, 0, oo2, 1, oo.length);
		return oo2;
	}
}
