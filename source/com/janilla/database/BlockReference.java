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

public record BlockReference(long self, long position, int capacity) {

	public static int BYTES = Long.BYTES + Integer.BYTES;

	public static BlockReference read(SeekableByteChannel channel) throws IOException {
		var p = channel.position();
		var b = ByteBuffer.allocate(BYTES);
		var n = channel.read(b);
		if (n != -1 && b.remaining() != 0)
			throw new IOException();
		return new BlockReference(p, b.getLong(0), b.getInt(Long.BYTES));
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
