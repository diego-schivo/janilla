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

import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.stream.IntStream;

public class FreeblockList<C extends Cell> extends AbstractSequentialList<Freeblock> {

	private final BTreePage<C> page;

	public FreeblockList(BTreePage<C> page) {
		this.page = page;
	}

	public void insert(Freeblock freeblock) {
		if (freeblock.start() == page.getCellContentAreaStart()) {
			var s = freeblock.end();
			Arrays.fill(page.buffer().array(), freeblock.start(), s, (byte) 0);
			var f = !isEmpty() ? getFirst() : null;
			var d = f != null ? f.start() - s : 4;
			if (d < 4) {
				s = f.end();
				removeFirst();
				if (d > 0)
					page.setFragmentedSize(page.getFragmentedSize() - d);
			}
			page.setCellContentAreaStart(s);
		} else {
			var fi = insertionIndex(freeblock);
			var a = true;
			if (!isEmpty()) {
				if (fi != 0) {
					var f1 = get(fi - 1);
					var f2 = freeblock;
					var d = f2.start() - f1.end();
					if (d < 4) {
						freeblock = new SimpleFreeblock(f1.start(), f2.end() - f1.start());
						set(fi - 1, freeblock);
						if (d > 0)
							page.setFragmentedSize(page.getFragmentedSize() - d);
						a = false;
					}
				}
				if (fi != size()) {
					var f1 = freeblock;
					var f2 = get(fi);
					var d = f2.start() - f1.end();
					if (d < 4) {
						freeblock = new SimpleFreeblock(f1.start(), f2.end() - f1.start());
						set(fi, freeblock);
						if (d > 0)
							page.setFragmentedSize(page.getFragmentedSize() - d);
						a = false;
					}
				}
			}
			if (a)
				add(fi, freeblock);
		}
	}

	public int insertionIndex(Freeblock freeblock) {
		return (int) starts().takeWhile(x -> x < freeblock.start()).count();
	}

	public int minSizeIndex(int size) {
		var ss = starts().map(x -> Short.toUnsignedInt(page.buffer().getShort(x + Short.BYTES)));
		var ii = new int[] { -1 };
		return ss.peek(_ -> ii[0]++).filter(x -> x >= size).findFirst().isPresent() ? ii[0] : -1;
	}

	@Override
	public boolean isEmpty() {
		return !starts().findFirst().isPresent();
	}

	@Override
	public int size() {
		return (int) starts().count();
	}

	@Override
	public ListIterator<Freeblock> listIterator(int index) {
		return new FreeblockIterator<>(page,
				index != 0 ? starts().skip(index - 1).findFirst().getAsInt() : page.buffer().position() + 1);
	}

	@Override
	public void clear() {
		page.buffer().putShort(page.buffer().position() + 1, (short) 0);
	}

	private IntStream starts() {
		return IntStream.iterate(page.buffer().position() + 1, x -> Short.toUnsignedInt(page.buffer().getShort(x)))
				.skip(1).takeWhile(x -> x != 0);
	}
}
