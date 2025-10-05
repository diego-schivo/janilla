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
import java.util.Arrays;

public abstract class BTree {

	protected final SQLiteDatabase database;

	protected final long rootNumber;

	protected BTree(SQLiteDatabase database, long rootNumber) {
		this.database = database;
		this.rootNumber = rootNumber;
	}

	protected byte[] payload(PayloadCell cell) {
		var bb = cell.initialPayload();
		if (cell.payloadSize() == bb.length)
			return bb;
		var b = ByteBuffer.allocate(cell.payloadSize());
		b.put(bb);
		var n = cell.firstOverflow();
		var pb = database.allocatePage();
		do {
			database.read(n, pb);
			var p = new OverflowPage(database, pb);
			p.getContent(b);
			n = p.getNext();
		} while (n != 0);
		return b.array();
	}

	protected long addOverflowPages(byte[] bytes, int start) {
		var b = ByteBuffer.wrap(bytes);
		b.position(start);
		var n = database.nextPageNumber();
		var n0 = n;
		ByteBuffer pb = null;
		do {
			if (pb == null)
				pb = database.allocatePage();
			else
				Arrays.fill(pb.array(), 0, pb.capacity(), (byte) 0);
			var p = new OverflowPage(database, pb);
			p.setContent(b);
			p.setNext(!b.hasRemaining() ? 0 : database.nextPageNumber());
			database.write(n, pb);
			n = p.getNext();
		} while (n != 0);
		return n0;
	}

	protected void removeOverflowPages(long number) {
		if (database.header.getFreelistSize() != 0)
			throw new RuntimeException();
		else {
			var b = database.allocatePage();
			database.read(number, b);
			var p = new FreelistPage(database, b);
			p.setNext(0);
			p.getLeafPointers().clear();
			database.write(number, b);
			database.header.setFreelistSize(1);
			database.header.setFreelistStart(number);
		}
	}
}
