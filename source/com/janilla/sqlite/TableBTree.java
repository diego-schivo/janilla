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

import java.util.ArrayList;
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
		var pp = new Path();
		for (;;) {
			var p = database.readBTreePage(n, database.allocatePage());
			pp.add(new Position(p, p.getCellCount()));
			if (p instanceof TableInteriorPage p2)
				n = p2.getRightMostPointer();
			else
				break;
		}

		var pi = pp.removeLast();
		var p = (TableLeafPage) pi.page();
		var i = pi.index();
		var k = (p.getCellCount() != 0 ? p.getCells().getLast().key() : 0) + 1;
		var vv = values.apply(k);
//		IO.println("TableBTree.insert, k=" + k + ", vv=" + Arrays.toString(vv));
		var l = (long) vv[0];
		if (l < k) {
//			throw new RuntimeException();
			var s = search(l);
			if (s.found() == s.path().size() - 1)
				return -1;
			pp = s.path();
			pi = pp.removeLast();
			p = (TableLeafPage) pi.page();
			i = pi.index();
		}
		k = l;
		var bb = Record.toBytes(Arrays.stream(vv).skip(1).map(Record.Element::of).toArray(Record.Element[]::new));
		var is = database.initialSize(bb.length, false);
		var c = is == bb.length ? new SimpleTableLeafCell(bb.length, k, bb, 0)
				: new SimpleTableLeafCell(bb.length, k, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		if (p.getCellCount() == 0)
			p.setCellContentAreaStart(database.usableSize());

		try {
			p.getCells().add(i, c);
			database.write(pp.number(pp.size()), p.buffer());
		} catch (IllegalStateException e) {
			var cc = new ArrayList<>(p.getCells());
			cc.add(i, c);
			new TableRebalance(pp, p, cc, database);
		}

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
	public boolean delete(Object... keys) {
//		IO.println("key=" + Arrays.toString(key));

		var k = (long) keys[0];
		var s = search(k);
		if (s.found() != s.path().size() - 1)
			return false;

		{
			var pi = s.path().removeLast();
			var p = (TableLeafPage) pi.page();
			var i = pi.index();
			p.getCells().remove(i);

			if (!s.path().isEmpty() && 3
					* (8 + p.getCells().stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize())
				new TableRebalance(s.path(), p, p.getCells(), database);
			else
				database.write(s.path().number(s.path().size()), p.buffer());
		}

		database.updateHeader();

		return true;
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
	public Object[] row(PayloadCell cell) {
		var oo = super.row(cell);
		if (oo[0] == null)
			oo[0] = ((TableLeafCell) cell).key();
		else {
			var oo2 = new Object[oo.length + 1];
			oo2[0] = ((TableLeafCell) cell).key();
			System.arraycopy(oo, 0, oo2, 1, oo.length);
			oo = oo2;
		}
		return oo;
	}

	protected Search search(long key) {
//		IO.println("keys=" + Arrays.toString(keys));
		var n = rootNumber;
		var f = -1;
		var pp = new Path();
		f: for (var i = 0;; i++) {
			var p = database.readBTreePage(n, database.allocatePage());
			var ci = 0;
			for (var c : p.getCells()) {
				var d = Long.compare(((KeyCell) c).key(), key);
				if (d == 0)
					f = i;
				if (d >= 0) {
					pp.add(new Position(p, ci));
					if (c instanceof InteriorCell c2) {
						n = c2.leftChildPointer();
						continue f;
					} else
						break f;
				}
				ci++;
			}
			pp.add(new Position(p, ci));
			if (p instanceof InteriorPage p2)
				n = p2.getRightMostPointer();
			else
				break;
		}
		return new Search(pp, f);
	}

	public record Search(Path path, int found) {
	}
}
