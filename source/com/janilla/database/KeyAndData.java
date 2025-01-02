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
package com.janilla.database;

import java.nio.ByteBuffer;

import com.janilla.io.ElementHelper;

public record KeyAndData<K>(K key, BlockReference attributes, BlockReference btree) {

	static <K> ElementHelper<KeyAndData<K>> getHelper(ElementHelper<K> keyHelper) {
		return new ElementHelper<>() {

			@Override
			public byte[] getBytes(KeyAndData<K> element) {
				var bb = keyHelper.getBytes(element.key());
				var b = ByteBuffer.allocate(bb.length + 2 * BlockReference.BYTES);
				b.put(bb);
				b.putLong(element.attributes.position());
				b.putInt(element.attributes.capacity());
				b.putLong(element.btree.position());
				b.putInt(element.btree.capacity());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return keyHelper.getLength(buffer) + 2 * BlockReference.BYTES;
			}

			@Override
			public KeyAndData<K> getElement(ByteBuffer buffer) {
				var k = keyHelper.getElement(buffer);
				var p1 = buffer.getLong();
				var c1 = buffer.getInt();
				var p2 = buffer.getLong();
				var c2 = buffer.getInt();
				return new KeyAndData<>(k, new BlockReference(-1, p1, c1), new BlockReference(-1, p2, c2));
			}

			@Override
			public int compare(ByteBuffer buffer, KeyAndData<K> element) {
				return keyHelper.compare(buffer, element.key());
			}
		};
	}
}
