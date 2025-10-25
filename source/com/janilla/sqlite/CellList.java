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
import java.util.Comparator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.stream.IntStream;

public class CellList<C extends Cell> extends AbstractList<C> implements RandomAccess {

	protected final BTreePage<C> page;

	public CellList(BTreePage<C> page) {
		this.page = page;
	}

	@Override
	public int size() {
		return page.getCellCount();
	}

	@Override
	public C get(int index) {
		Objects.checkIndex(index, page.getCellCount());
		return page.cell(index);
	}

	@Override
	public void add(int index, C element) {
		var cp = addCell(element);
		if (cp == -1)
			throw new IllegalStateException();
		page.cellPointers.add(index, cp);
		page.setCellCount(page.getCellCount() + 1);
	}

	@Override
	public C remove(int index) {
		var c = get(index);
		removeCell(c);
		page.cellPointers.remove(index);
		page.setCellCount(page.getCellCount() - 1);
		return c;
	}

//	@Override
//	public C set(int index, C element) {
//		var c = get(index);
//		var d = element.size() - c.size();
//		int cp;
//		if (d > 0) {
//			removeCell(c);
//			cp = addCell(element);
//			if (cp == -1)
//				throw new IllegalStateException();
//		} else {
//			d = -d;
//			cp = c.start() + d;
//			var p0 = page.buffer.position();
//			page.buffer.position(cp);
//			element.put(page.buffer);
//			page.buffer.position(p0);
//			if (d == 0)
//				;
//			else if (d < 4)
//				page.setFragmentedSize(page.getFragmentedSize() + d);
//			else
//				page.freeblocks.insert(new Freeblock.New(c.start(), d));
//		}
//		page.cellPointers.set(index, cp);
//		return c;
//	}

	protected int addCell(C cell) {
		var s = page.getCellContentAreaStart() - (page.buffer.position() + (page instanceof InteriorPage ? 12 : 8)
				+ page.getCellCount() * Short.BYTES);
		var cs = cell.size();
		var fi = s >= Short.BYTES ? page.freeblocks.minSizeIndex(cs) : -1;
		int cp;
		if (fi != -1) {
//				IO.println("fi=" + fi);
			var f = page.freeblocks.get(fi);
			var d = f.size() - cs;
			if (d >= 4) {
				cp = f.start() + d;
				page.freeblocks.set(fi, new Freeblock.New(f.start(), d));
			} else {
				cp = f.start();
				page.freeblocks.remove(fi);
				if (d != 0)
					page.setFragmentedSize(page.getFragmentedSize() + d);
			}
		} else {
			if (s < Short.BYTES + cs) {
				s += page.freeblocks.stream().mapToInt(x -> x.size()).sum();
				if (s < Short.BYTES + cs) {
//					IO.println(
//							"s < Short.BYTES + cs, " + s + ", " + (Short.BYTES + cs) + ", " + page.getFragmentedSize());
					if (s + page.getFragmentedSize() < Short.BYTES + cs)
						return -1;
					page.freeblocks.clear();
					page.setCellContentAreaStart(page.database.usableSize());
					page.setFragmentedSize(0);
					record R(int i, Cell c) {
					}
					IntStream.range(0, page.getCellCount()).mapToObj(x -> new R(x, page.getCells().get(x)))
							.sorted(Comparator.<R>comparingInt(x -> x.c.start()).reversed()).forEach(x -> {
								var l = x.c.size();
								var dp = page.getCellContentAreaStart() - l;
								System.arraycopy(page.buffer.array(), x.c.start(), page.buffer.array(), dp, l);
								page.cellPointers.set(x.i, dp);
								page.setCellContentAreaStart(dp);
							});
				}
				for (var ff = page.freeblocks.iterator(); ff.hasNext();) {
					var f = ff.next();
					var fs = f.start();
					var d = f.size();
					ff.remove();
					var as1 = page.getCellContentAreaStart();
					var as2 = as1 + d;
					System.arraycopy(page.buffer.array(), as1, page.buffer.array(), as2, fs - as1);
					for (var i = 0; i < page.cellPointers.size(); i++) {
						var p = page.cellPointers.get(i).intValue();
						if (p < fs) {
							p += d;
							page.cellPointers.set(i, p);
						}
					}
					page.setCellContentAreaStart(as2);
				}
			}
			cp = page.getCellContentAreaStart() - cs;
			page.setCellContentAreaStart(cp);
		}
		var p0 = page.buffer.position();
		page.buffer.position(cp);
		cell.put(page.buffer);
		page.buffer.position(p0);
		return cp;
	}

	protected void removeCell(C cell) {
		page.freeblocks.insert(new Freeblock.New(cell.start(), cell.size()));
	}
}
