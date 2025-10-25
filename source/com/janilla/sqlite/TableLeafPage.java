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

public class TableLeafPage extends BTreePage<TableLeafCell> {

	public TableLeafPage(SQLiteDatabase database, ByteBuffer buffer) {
		super(database, buffer);
		setType(0x0d);
	}

	@Override
	protected TableLeafCell cell(int index) {
		return new TableLeafCell() {

			public int pointer() {
				return buffer.position() + 8 + index * Short.BYTES;
			}

//			@Override
//			public BTreePage<?> page() {
//				return LeafTablePage.this;
//			}

			@Override
			public int start() {
				return Short.toUnsignedInt(buffer.getShort(pointer()));
			}

			@Override
			public int payloadSize() {
				return (int) Varint.get(buffer, start());
			}

			@Override
			public long key() {
				var i = start();
				i += Varint.size(buffer, i);
				return Varint.get(buffer, i);
			}

			@Override
			public int initialPayloadSize() {
				return database.initialSize(payloadSize(), false);
			}

			@Override
			public void getInitialPayload(ByteBuffer destination) {
				var ps = payloadSize();
				var is = database.initialSize(ps, false);
				var i = start() + Varint.size(ps);
				destination.put(buffer.array(), i + Varint.size(buffer, i), is);
			}

			@Override
			public long firstOverflow() {
				var ps = payloadSize();
				var is = database.initialSize(ps, false);
				var i = start() + Varint.size(ps);
				i += Varint.size(buffer, i);
				return is != ps ? buffer.getInt(i + is) : 0;
			}

			@Override
			public int size() {
				var ps = payloadSize();
				var is = database.initialSize(ps, false);
				var s = Varint.size(ps);
				return s + Varint.size(buffer, start() + s) + is + (is != ps ? Integer.BYTES : 0);
			}
		};
	}
}
