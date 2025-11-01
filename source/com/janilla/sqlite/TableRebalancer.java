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
import java.util.List;
import java.util.stream.IntStream;

public class TableRebalancer implements Runnable {

	protected final BTreePath path;

	protected final BTreePage<? extends KeyCell> page;

	protected final List<? extends KeyCell> cells;

	protected final SqliteDatabase database;

	protected TableInteriorPage p1;

	public TableRebalancer(BTreePath path, BTreePage<? extends KeyCell> page, List<? extends KeyCell> cells) {
		this.path = path;
		this.page = page;
		this.cells = cells;
		database = page.database;
	}

	@Override
	public void run() {
//		IO.println("TableRebalancer.run, path=" + path);
		if (!path.isEmpty())
			p1 = (TableInteriorPage) path.getLast().page();

		record R(int i, long n, BTreePage<? extends KeyCell> p) {
		}
		R[] rr;
		if (path.isEmpty())
			rr = new R[] { new R(0, path.pointer(0), page) };
		else {
			var i = path.getLast().index();
			var l = p1.getCellCount() + 1;
			var i1 = Math.max(0, i == l - 1 ? l - 3 : i - 1);
			var i2 = Math.min(i1 + 2, l - 1);
			rr = IntStream.rangeClosed(i1, i2).mapToObj(x -> {
				var n = p1.childPointer(x);
				@SuppressWarnings("unchecked")
				var p = x == i ? page : (BTreePage<? extends KeyCell>) BTreePage.read(n, database);
				return new R(x, n, p);
			}).toArray(R[]::new);
		}
//		IO.println("TableRebalancer.run, rr=" + Arrays.toString(rr));

		var cc2 = Arrays.stream(rr).flatMap(x -> x.p == page ? cells.stream() : x.p.getCells().stream()).toList();
//		IO.println("TableRebalancer.run, cc2=" + cc2);
		var ll = partition(cc2.stream().mapToInt(x -> Short.BYTES + x.size()).toArray(), page instanceof InteriorPage);
//		IO.println("TableRebalancer.run, ll=" + Arrays.toString(ll));
		if (p1 == null) {
			var b = database.newPageBuffer();
			if (path.isEmpty() && rr[0].n == 1)
				b.position(100);
			p1 = new TableInteriorPage(database, b);
			p1.setCellContentAreaStart(database.usableSize());
		}
		var cc1 = p1.getCells();

		var i2 = 0;
		for (var li = 0; li < ll.length; li++) {
			var n = !path.isEmpty() && li < rr.length ? rr[li].n : database.newPage();
			BTreePage<? extends KeyCell> p;
			{
				p = page instanceof InteriorPage ? new TableInteriorPage(database, database.newPageBuffer())
						: new TableLeafPage(database, database.newPageBuffer());
				p.setCellContentAreaStart(database.usableSize());
				((List) p.getCells()).addAll(cc2.subList(i2, i2 += ll[li]));
				if (p instanceof TableInteriorPage x)
					x.setRightMostPointer(i2 != cc2.size() ? ((TableInteriorCell) cc2.get(i2)).leftChildPointer()
							: ((TableInteriorPage) rr[rr.length - 1].p).getRightMostPointer());
				database.writePageBuffer(n, p.buffer());
			}
			var i1 = rr[0].i + li;
			if (i2 != cc2.size()) {
				var x = (KeyCell) cc2.get(page instanceof InteriorPage ? i2++ : i2 - 1);
				var y = new SimpleTableInteriorCell(n, x.key());
				if (!path.isEmpty() && li < rr.length - 1)
					cc1.remove(i1);
				try {
					cc1.add(i1, y);
				} catch (IllegalStateException e) {
					cc1 = new ArrayList<>(cc1);
					cc1.add(i1, y);
				}
			} else if (ll.length != rr.length) {
				for (var ri = ll.length; ri < rr.length; ri++) {
					cc1.remove(i1);
					database.freePage(rr[ri].n, rr[ri].p.buffer());
				}
				if (!path.isEmpty() && i1 != cc1.size()) {
					var x = cc1.get(i1);
					var y = new SimpleTableInteriorCell(n, x.key());
					cc1.remove(i1);
					cc1.add(i1, y);
				} else if (path.size() == 1 && p1.getCellCount() == 0) {
					database.writePageBuffer(path.pointer(0), p.buffer());
					database.freePage(n, p.buffer());
					p1 = null;
				} else
					p1.setRightMostPointer(n);
			} else if (path.isEmpty())
				p1.setRightMostPointer(n);
		}

		if (p1 == null)
			;
		else if (cc1 == p1.getCells() && !(ll.length < rr.length && path.size() > 1
				&& 3 * (12 + cc1.stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize()))
			database.writePageBuffer(path.pointer(!path.isEmpty() ? path.size() - 1 : 0), p1.buffer());
		else {
			path.removeLast();
			new TableRebalancer(path, p1, cc1);
		}
	}

	protected int[] partition(int[] cellSizes, boolean interior) {
//		IO.println("cellSizes=" + Arrays.toString(cellSizes) + " " + cellSizes.length);
		var s = Arrays.stream(cellSizes).sum();
		var u = database.usableSize() - (interior ? 12 : 8);
		var l = Math.ceilDiv(s, u);
//		IO.println("s=" + s + ", u=" + u + ", l=" + l);
		var ii = new int[l];
		if (interior) {
			var d = (double) s / l;
			s = 0;
			var i = 0;
			for (var si = 0; si < cellSizes.length; si++) {
				s += cellSizes[si];
				if (s > (i + 1) * d) {
//					IO.println("s=" + s + ", d2=" + d2);
					i++;
				} else
					ii[i]++;
			}
		} else {
//			s = 0;
//			var i = 0;
//			for (var si = 0; si < cellSizes.length; si++) {
//				s += cellSizes[si];
//				if (s > y) {
			////				IO.println("s=" + s + ", d2=" + d2);
//					if (++i == ii.length)
//						ii = Arrays.copyOf(ii, i + 1);
//					s = cellSizes[si];
//				}
//				ii[i]++;
//			}
			var d = (double) s / l;
			s = 0;
			var i = 0;
			for (var si = 0; si < cellSizes.length; si++) {
				s += cellSizes[si];
				if (s > u) {
					if (++i == ii.length)
						ii = Arrays.copyOf(ii, i + 1);
					s = cellSizes[si];
				}
				ii[i]++;
				if (s > d) {
//					IO.println("s=" + s + ", d=" + d);
					if (++i == ii.length && si != cellSizes.length - 1)
						ii = Arrays.copyOf(ii, i + 1);
					s = 0;
				}
			}
			if (i < l - 1)
				ii = Arrays.copyOf(ii, i + 1);
		}
//		IO.println("ii=" + Arrays.toString(ii));
		return ii;
	}
}
