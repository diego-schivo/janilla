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

public record IdAndElement(long id, BlockReference element) {

	static int BYTES = Long.BYTES + BlockReference.BYTES;

	static ElementHelper<IdAndElement> HELPER = new ElementHelper<>() {

		@Override
		public byte[] getBytes(IdAndElement element) {
			var b = ByteBuffer.allocate(BYTES);
			b.putLong(element.id());
			b.putLong(element.element.position());
			b.putInt(element.element.capacity());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return BYTES;
		}

		@Override
		public IdAndElement getElement(ByteBuffer buffer) {
			var i = buffer.getLong();
			var q = buffer.getLong();
			var c = buffer.getInt();
			return new IdAndElement(i, new BlockReference(-1, q, c));
		}

		@Override
		public int compare(ByteBuffer buffer, IdAndElement element) {
			return Long.compare(buffer.getLong(buffer.position()), element.id());
		}
	};
}
