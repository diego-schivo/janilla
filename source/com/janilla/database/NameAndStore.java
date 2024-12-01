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

public record NameAndStore(String name, BlockReference root, IdAndSize idAndSize) {

	static ElementHelper<NameAndStore> HELPER = new ElementHelper<>() {

		@Override
		public byte[] getBytes(NameAndStore element) {
			var bb = element.name().getBytes();
			var b = ByteBuffer.allocate(Integer.BYTES + bb.length + BlockReference.HELPER_LENGTH + IdAndSize.HELPER_LENGTH);
			b.putInt(bb.length);
			b.put(bb);
			b.putLong(element.root.position());
			b.putInt(element.root.capacity());
			b.putLong(element.idAndSize.id());
			b.putLong(element.idAndSize.size());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Integer.BYTES + buffer.getInt(buffer.position()) + BlockReference.HELPER_LENGTH + IdAndSize.HELPER_LENGTH;
		}

		@Override
		public NameAndStore getElement(ByteBuffer buffer) {
			var bb = new byte[buffer.getInt()];
			buffer.get(bb);
			var p = buffer.getLong();
			var c = buffer.getInt();
			var id = buffer.getLong();
			var s = buffer.getLong();
			return new NameAndStore(new String(bb), new BlockReference(-1, p, c), new IdAndSize(id, s));
		}

		@Override
		public int compare(ByteBuffer buffer, NameAndStore element) {
			var p = buffer.position();
			var bb = new byte[buffer.getInt(p)];
			buffer.get(p + Integer.BYTES, bb);
			return new String(bb).compareTo(element.name());
		}
	};
}
