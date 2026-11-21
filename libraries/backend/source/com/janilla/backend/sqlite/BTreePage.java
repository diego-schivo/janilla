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

import java.nio.ByteBuffer;
import java.util.List;

public sealed abstract class BTreePage<C extends Cell> extends Page permits InteriorPage, TableLeafPage, IndexLeafPage {

	public static BTreePage<?> of(ByteBuffer buffer, SqliteDatabase database) {
		var t = Byte.toUnsignedInt(buffer.get(buffer.position()));
		return switch (t) {
		case 0x05 -> new TableInteriorPage(database, buffer);
		case 0x0d -> new TableLeafPage(database, buffer);
		case 0x02 -> new IndexInteriorPage(database, buffer);
		case 0x0a -> new IndexLeafPage(database, buffer);
		default -> throw new RuntimeException(Integer.toHexString(t));
		};
	}

	public static BTreePage<?> read(long number, SqliteDatabase database) {
		var b = database.newPageBuffer();
		database.readPageBuffer(number, b);
		return of(b, database);
	}

	private final FreeblockList<C> freeblocks = new FreeblockList<C>(this);

	private final CellPointerList<C> cellPointers = new CellPointerList<C>(this);

	private final CellList<C> cells = new CellList<C>(this);

	protected BTreePage(SqliteDatabase database, ByteBuffer buffer) {
		super(database, buffer);
	}

	public int getType() {
		return Byte.toUnsignedInt(buffer.get(buffer.position()));
	}

	public void setType(int type) {
		buffer.put(buffer.position(), (byte) type);
	}

	public List<Freeblock> getFreeblocks() {
		return freeblocks;
	}

	public int getCellCount() {
		return Short.toUnsignedInt(buffer.getShort(buffer.position() + 3));
	}

	public void setCellCount(int cellCount) {
		buffer.putShort(buffer.position() + 3, (short) cellCount);
	}

	public int getCellContentAreaStart() {
		return Short.toUnsignedInt(buffer.getShort(buffer.position() + 5));
	}

	public void setCellContentAreaStart(int cellContentAreaStart) {
		buffer.putShort(buffer.position() + 5, (short) cellContentAreaStart);
	}

	public int getFragmentedSize() {
		return Byte.toUnsignedInt(buffer.get(buffer.position() + 7));
	}

	public void setFragmentedSize(int fragmentedSize) {
		buffer.put(buffer.position() + 7, (byte) fragmentedSize);
	}

	public List<Integer> getCellPointers() {
		return cellPointers;
	}

	public List<C> getCells() {
		return cells;
	}

	protected abstract C cell(int index);

	public int selectFreeblock(int minSize) {
		return freeblocks.minSizeIndex(minSize);
	}

	public void insertFreeblock(Freeblock freeblock) {
		freeblocks.insert(freeblock);
	}
}
