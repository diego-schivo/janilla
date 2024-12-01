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

public record KeyAndValues<K>(K key, BlockReference root, long size) {

	static <K> ElementHelper<KeyAndValues<K>> getHelper(ElementHelper<K> keyHelper) {
		return new ElementHelper<>() {

			@Override
			public byte[] getBytes(KeyAndValues<K> element) {
				var bb = keyHelper.getBytes(element.key());
				var b = ByteBuffer.allocate(bb.length + BlockReference.HELPER_LENGTH + Long.BYTES);
				b.put(bb);
				var r = element.root();
				b.putLong(r.position());
				b.putInt(r.capacity());
				b.putLong(element.size());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return keyHelper.getLength(buffer) + BlockReference.HELPER_LENGTH + Long.BYTES;
			}

			@Override
			public KeyAndValues<K> getElement(ByteBuffer buffer) {
				var k = keyHelper.getElement(buffer);
				var p = buffer.getLong();
				var c = buffer.getInt();
				var s = buffer.getLong();
				return new KeyAndValues<>(k, new BlockReference(-1, p, c), s);
			}

			@Override
			public int compare(ByteBuffer buffer, KeyAndValues<K> element) {
				return keyHelper.compare(buffer, element.key());
			}
		};
	}
}
