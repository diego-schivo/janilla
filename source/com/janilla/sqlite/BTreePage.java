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

import java.nio.ByteBuffer;
import java.util.List;

public abstract class BTreePage<C extends Cell> extends Page {

	protected FreeblockList<C> freeblocks = new FreeblockList<C>(this);

	protected CellPointerList<C> cellPointers = new CellPointerList<C>(this);

	protected CellList<C> cells = new CellList<C>(this);

	protected BTreePage(SQLiteDatabase database, ByteBuffer buffer) {
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

	public List<C> getCells() {
		return cells;
	}

	protected abstract C cell(int index);
}
