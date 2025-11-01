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
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class FreeblockIterator<C extends Cell> implements ListIterator<Freeblock> {

	private final BTreePage<C> page;

	private int pointer;

	private int previousPointer;

	public FreeblockIterator(BTreePage<C> page, int pointer) {
		this.page = page;
		this.pointer = pointer;
	}

	@Override
	public boolean hasNext() {
		return page.buffer().getShort(pointer) != 0;
	}

	@Override
	public Freeblock next() {
		if (page.buffer().getShort(pointer) == 0)
			throw new NoSuchElementException();
		var i = pointer;
		var x = new Freeblock() {

			@Override
			public int start() {
				return Short.toUnsignedInt(page.buffer().getShort(i));
			}

			@Override
			public int size() {
				return Short.toUnsignedInt(page.buffer().getShort(start() + Short.BYTES));
			}
		};
		previousPointer = pointer;
		pointer = x.start();
		return x;
	}

	@Override
	public boolean hasPrevious() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Freeblock previous() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nextIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int previousIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove() {
		page.buffer().putShort(previousPointer, page.buffer().getShort(pointer));
		Arrays.fill(page.buffer().array(), pointer, pointer + 2 * Short.BYTES, (byte) 0);
		pointer = previousPointer;
	}

	@Override
	public void set(Freeblock e) {
		if (e.start() != pointer) {
			page.buffer().putShort(previousPointer, (short) e.start());
			page.buffer().putShort(e.start(), page.buffer().getShort(pointer));
//				Arrays.fill(buffer.array(), e.start() + 2 * Short.BYTES, pointer + 2 * Short.BYTES, (byte) 0);
			pointer = e.start();
		}
		page.buffer().putShort(pointer + Short.BYTES, (short) e.size());
		Arrays.fill(page.buffer().array(), e.start() + 2 * Short.BYTES, e.end(), (byte) 0);
	}

	@Override
	public void add(Freeblock e) {
		insert(e, pointer, Short.toUnsignedInt(page.buffer().getShort(pointer)));
	}

	private void insert(Freeblock e, int previous, int next) {
		page.buffer().putShort(previous, (short) e.start());
		page.buffer().putShort(e.start(), (short) next);
		page.buffer().putShort(e.start() + Short.BYTES, (short) e.size());
		Arrays.fill(page.buffer().array(), e.start() + 2 * Short.BYTES, e.start() + e.size(), (byte) 0);
	}
}
