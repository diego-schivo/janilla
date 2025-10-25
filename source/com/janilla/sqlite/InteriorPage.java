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
import java.util.Objects;

public abstract class InteriorPage<C extends InteriorCell> extends BTreePage<C> {

	protected InteriorPage(SQLiteDatabase database, ByteBuffer buffer) {
		super(database, buffer);
	}

	public long getRightMostPointer() {
		return Integer.toUnsignedLong(buffer.getInt(buffer.position() + 8));
	}

	public void setRightMostPointer(long rightMostPointer) {
		buffer.putInt(buffer.position() + 8, (int) rightMostPointer);
	}

	public long childPointer(int index) {
		var l = getCellCount() + 1;
		Objects.checkIndex(index, l);
		return index == l - 1 ? getRightMostPointer() : cells.get(index).leftChildPointer();
	}
}
