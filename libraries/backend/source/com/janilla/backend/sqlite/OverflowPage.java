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

public class OverflowPage extends Page {

	public OverflowPage(SqliteDatabase database, ByteBuffer buffer) {
		super(database, buffer);
	}

	public OverflowPage(SqliteDatabase database, long number) {
		super(database, number);
	}

	public long getNext() {
		return Integer.toUnsignedLong(buffer.getInt(0));
	}

	public void setNext(long next) {
		buffer.putInt(0, (int) next);
	}

	public void getContent(ByteBuffer destination) {
		destination.put(buffer.array(), Integer.BYTES,
				Math.min(database.usableSize() - Integer.BYTES, destination.remaining()));
	}

	public void setContent(ByteBuffer source) {
		var o = source.position();
		var l = Math.min(database.usableSize() - Integer.BYTES, source.remaining());
		buffer.put(Integer.BYTES, source.array(), o, l);
		source.position(o + l);
	}
}
