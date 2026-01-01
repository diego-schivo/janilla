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
package com.janilla.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IndexRebalancer implements Runnable {

	protected final BTreePath path;

	protected final BTreePage<? extends PayloadCell> page;

	protected final List<? extends PayloadCell> cells;

	protected final SqliteDatabase database;

	protected IndexInteriorPage p1;

	public IndexRebalancer(BTreePath path, BTreePage<? extends PayloadCell> page, List<? extends PayloadCell> cells) {
		this.path = path;
		this.page = page;
		this.cells = cells;
		database = page.database;
	}

	@Override
	public void run() {
		p1 = !path.isEmpty() ? (IndexInteriorPage) path.getLast().page() : null;

		record R(int i, long n, BTreePage<? extends PayloadCell> p) {
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
				var p = x == i ? page : (BTreePage<? extends PayloadCell>) BTreePage.read(n, database);
				return new R(x, n, p);
			}).toArray(R[]::new);
		}
		var cc2 = Arrays.stream(rr)
				.flatMap(x -> Stream.concat(x.p == page ? cells.stream() : x.p.getCells().stream(),
						Optional.ofNullable(x != rr[rr.length - 1] ? p1.getCells().get(x.i) : null)
								.map(y -> page instanceof InteriorPage
										? new SimpleIndexInteriorCell(((IndexInteriorPage) x.p).getRightMostPointer(),
												y.payloadSize(), y.toInitialPayloadBytes(), y.firstOverflow())
										: new SimpleIndexLeafCell(y.payloadSize(), y.toInitialPayloadBytes(),
												y.firstOverflow()))
								.stream()))
				.toList();

		var ll = partition(cc2.stream().mapToInt(x -> Short.BYTES + x.size()).toArray(), page instanceof InteriorPage);
		if (p1 == null && ll.length != 1) {
			p1 = new IndexInteriorPage(database, database.newPageBuffer());
			p1.setCellContentAreaStart(database.usableSize());
		}
		var cc1 = p1 != null ? p1.getCells() : null;

		var i2 = 0;
		for (var li = 0; li < ll.length; li++) {
			var n = li < rr.length && (!path.isEmpty() || p1 == null) ? rr[li].n : database.newPage();
			{
				var x = page instanceof InteriorPage ? new IndexInteriorPage(database, database.newPageBuffer())
						: new IndexLeafPage(database, database.newPageBuffer());
				x.setCellContentAreaStart(database.usableSize());
				((List) x.getCells()).addAll(cc2.subList(i2, i2 += ll[li]));
				if (x instanceof IndexInteriorPage y)
					y.setRightMostPointer(i2 != cc2.size() ? ((IndexInteriorCell) cc2.get(i2)).leftChildPointer()
							: ((IndexInteriorPage) rr[rr.length - 1].p).getRightMostPointer());
				database.writePageBuffer(n, x.buffer());
			}
			if (i2 != cc2.size()) {
				var x = cc2.get(i2++);
				var y = new SimpleIndexInteriorCell(n, x.payloadSize(), x.toInitialPayloadBytes(), x.firstOverflow());
				if (!path.isEmpty() && li < rr.length - 1)
					cc1.remove(rr[li].i);
				try {
					cc1.add(rr[0].i + li, y);
				} catch (IllegalStateException e) {
					cc1 = new ArrayList<>(cc1);
					cc1.add(rr[0].i + li, y);
				}
			} else if (ll.length != rr.length) {
				for (var ri = ll.length; ri < rr.length; ri++) {
					cc1.remove(rr[ll.length - 1].i);
					database.freePage(rr[ri].n, rr[ri].p.buffer());
				}
				if (!path.isEmpty() && rr[0].i + li != cc1.size()) {
					var x = cc1.get(rr[0].i + li);
					var y = new SimpleIndexInteriorCell(n, x.payloadSize(), x.toInitialPayloadBytes(),
							x.firstOverflow());
					cc1.remove(rr[0].i + li);
					cc1.add(rr[0].i + li, y);
				} else
					p1.setRightMostPointer(n);
			}
		}

		if (p1 == null)
			;
		else if (cc1 == p1.getCells() && !(ll.length < rr.length && path.size() > 1
				&& 3 * (12 + cc1.stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize()))
			database.writePageBuffer(path.pointer(!path.isEmpty() ? path.size() - 1 : 0), p1.buffer());
		else {
			path.removeLast();
			new IndexRebalancer(path, p1, cc1);
		}
	}

	protected int[] partition(int[] cellSizes, boolean interior) {
//		IO.println("ss=" + Arrays.toString(ss) + " " + ss.length);
		var s = Arrays.stream(cellSizes).sum();
//		IO.println("s=" + s + ", u=" + u);
		var l = Math.ceilDiv(s, database.usableSize() - (interior ? 12 : 8));
		var d = (double) s / l;
		var ii = new int[l];
		s = 0;
		var i = 0;
		for (var si = 0; si < cellSizes.length; si++) {
			s += cellSizes[si];
			if (s > (i + 1) * d) {
//				IO.println("s=" + s + ", d2=" + d2);
				i++;
			} else
				ii[i]++;
		}
//		IO.println("ii=" + Arrays.toString(ii));
		return ii;
	}
}
