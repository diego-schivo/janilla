/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
package com.janilla.database;

import java.nio.ByteBuffer;

import com.janilla.io.ElementHelper;

public record NameAndData(String name, BlockReference attributes, BlockReference btree) {

	static ElementHelper<NameAndData> HELPER = new ElementHelper<>() {

		@Override
		public byte[] getBytes(NameAndData element) {
			var bb = element.name().getBytes();
			var b = ByteBuffer.allocate(Integer.BYTES + bb.length + 2 * BlockReference.BYTES);
			b.putInt(bb.length);
			b.put(bb);
			b.putLong(element.attributes.position());
			b.putInt(element.attributes.capacity());
			b.putLong(element.btree.position());
			b.putInt(element.btree.capacity());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Integer.BYTES + buffer.getInt(buffer.position()) + 2 * BlockReference.BYTES;
		}

		@Override
		public NameAndData getElement(ByteBuffer buffer) {
			var bb = new byte[buffer.getInt()];
			buffer.get(bb);
			var p1 = buffer.getLong();
			var c1 = buffer.getInt();
			var p2 = buffer.getLong();
			var c2 = buffer.getInt();
			return new NameAndData(new String(bb), new BlockReference(-1, p1, c1), new BlockReference(-1, p2, c2));
		}

		@Override
		public int compare(ByteBuffer buffer, NameAndData element) {
			var p = buffer.position();
			var bb = new byte[buffer.getInt(p)];
			buffer.get(p + Integer.BYTES, bb);
			return new String(bb).compareTo(element.name());
		}
	};
}
