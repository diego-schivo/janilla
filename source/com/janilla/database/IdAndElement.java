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

public record IdAndElement<T extends Comparable<T>>(T id, BlockReference element) {

//	public static int BYTES = Long.BYTES + BlockReference.BYTES;
//
//	public static ByteConverter<IdAndElement> BYTE_CONVERTER = new ByteConverter<>() {
//
//		@Override
//		public byte[] serialize(IdAndElement element) {
//			var b = ByteBuffer.allocate(BYTES);
//			b.putLong(element.id());
//			b.putLong(element.element.position());
//			b.putInt(element.element.capacity());
//			return b.array();
//		}
//
//		@Override
//		public int getLength(ByteBuffer buffer) {
//			return BYTES;
//		}
//
//		@Override
//		public IdAndElement deserialize(ByteBuffer buffer) {
//			var i = buffer.getLong();
//			var q = buffer.getLong();
//			var c = buffer.getInt();
//			return new IdAndElement(i, new BlockReference(-1, q, c));
//		}
//
//		@Override
//		public int compare(ByteBuffer buffer, IdAndElement element) {
//			return Long.compare(buffer.getLong(buffer.position()), element.id());
//		}
//	};

	public static <T extends Comparable<T>> ByteConverter<IdAndElement<T>> byteConverter(
			ByteConverter<T> idByteConverter) {
		return new ByteConverter<>() {

			@Override
			public byte[] serialize(IdAndElement<T> element) {
				var bb = idByteConverter.serialize(element.id);
				var b = ByteBuffer.allocate(bb.length + BlockReference.BYTES);
				b.put(bb);
				b.putLong(element.element.position());
				b.putInt(element.element.capacity());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return idByteConverter.getLength(buffer) + BlockReference.BYTES;
			}

			@Override
			public IdAndElement<T> deserialize(ByteBuffer buffer) {
				var i = idByteConverter.deserialize(buffer);
				var q = buffer.getLong();
				var c = buffer.getInt();
				return new IdAndElement<>(i, new BlockReference(-1, q, c));
			}

			@Override
			public int compare(ByteBuffer buffer, IdAndElement<T> element) {
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
