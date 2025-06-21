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

import com.janilla.io.ByteConverter;

public record IdAndReference<ID extends Comparable<ID>>(ID id, BlockReference reference) {

	public static <ID extends Comparable<ID>> ByteConverter<IdAndReference<ID>> byteConverter(
			ByteConverter<ID> idByteConverter) {
		return new ByteConverter<>() {

			@Override
			public byte[] serialize(IdAndReference<ID> element) {
				var bb = idByteConverter.serialize(element.id);
				var b = ByteBuffer.allocate(bb.length + BlockReference.BYTES);
				b.put(bb);
				b.putLong(element.reference.position());
				b.putInt(element.reference.capacity());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return idByteConverter.getLength(buffer) + BlockReference.BYTES;
			}

			@Override
			public IdAndReference<ID> deserialize(ByteBuffer buffer) {
				var i = idByteConverter.deserialize(buffer);
				var q = buffer.getLong();
				var c = buffer.getInt();
				return new IdAndReference<>(i, new BlockReference(-1, q, c));
			}

			@Override
			public int compare(ByteBuffer buffer, IdAndReference<ID> element) {
				var p = buffer.position();
				try {
					var id = idByteConverter.deserialize(buffer);
					return id != null ? id.compareTo(element.id) : element.id != null ? -1 : 0;
				} finally {
					buffer.position(p);
				}
			}
		};
	}
}
