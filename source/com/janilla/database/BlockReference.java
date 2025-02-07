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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.janilla.io.ByteConverter;

public record BlockReference(long self, long position, int capacity) {

	public static int BYTES = Long.BYTES + Integer.BYTES;

	public static ByteConverter<BlockReference> BYTE_CONVERTER = new ByteConverter<BlockReference>() {

		@Override
		public byte[] serialize(BlockReference element) {
			var b = ByteBuffer.allocate(BYTES);
			b.putLong(element.position);
			b.putInt(element.capacity);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return BYTES;
		}

		@Override
		public BlockReference deserialize(ByteBuffer buffer) {
			var p = buffer.getLong();
			var c = buffer.getInt();
			return new BlockReference(-1, p, c);
		}

		@Override
		public int compare(ByteBuffer buffer, BlockReference element) {
			var p = buffer.position();
			var c = buffer.getInt(p + Long.BYTES);
			var i = Integer.compare(c, element.capacity);
			if (i == 0 && element.position >= 0) {
				var q = buffer.getLong(p);
				i = Long.compare(q, element.position);
			}
			return i;
		}
	};

	public static BlockReference read(SeekableByteChannel channel, long position) throws IOException {
		channel.position(position);
		var b = ByteBuffer.allocate(BYTES);
		var n = channel.read(b);
		if (n != -1 && b.remaining() != 0)
			throw new IOException();
		return new BlockReference(position, b.getLong(0), b.getInt(Long.BYTES));
	}

	public static void write(BlockReference reference, SeekableByteChannel channel) throws IOException {
		channel.position(reference.self());
		var b = ByteBuffer.allocate(BYTES);
		b.putLong(0, reference.position());
		b.putInt(Long.BYTES, reference.capacity());
		channel.write(b);
		if (b.remaining() != 0)
			throw new IOException();
	}
}
