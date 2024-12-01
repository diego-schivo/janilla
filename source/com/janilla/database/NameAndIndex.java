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

public record NameAndIndex(String name, BlockReference root) {

	static ElementHelper<NameAndIndex> HELPER = new ElementHelper<>() {

		@Override
		public byte[] getBytes(NameAndIndex element) {
			var b = element.name().getBytes();
			var c = ByteBuffer.allocate(4 + b.length + BlockReference.HELPER_LENGTH);
			c.putInt(b.length);
			c.put(b);
			c.putLong(element.root.position());
			c.putInt(element.root.capacity());
			return c.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 4 + buffer.getInt(buffer.position()) + BlockReference.HELPER_LENGTH;
		}

		@Override
		public NameAndIndex getElement(ByteBuffer buffer) {
			var b = new byte[buffer.getInt()];
			buffer.get(b);
			var p = buffer.getLong();
			var c = buffer.getInt();
			return new NameAndIndex(new String(b), new BlockReference(-1, p, c));
		}

		@Override
		public int compare(ByteBuffer buffer, NameAndIndex element) {
			var p = buffer.position();
			var b = new byte[buffer.getInt(p)];
			buffer.get(p + 4, b);
			return new String(b).compareTo(element.name());
		}
	};
}
