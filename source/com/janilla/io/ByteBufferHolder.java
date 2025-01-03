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
package com.janilla.io;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ByteBufferHolder {

	public static void main(String[] args) {
		var h = new ByteBufferHolder();
		h.setBuffer(ByteBuffer.allocate(10));
		try {
			h.getBuffer().put("foobarbazqux".getBytes());
			assert false;
		} catch (BufferOverflowException e) {
		}

		h.grow();
		h.getBuffer().put("foobarbazqux".getBytes());
	}

	ByteBuffer buffer;

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public ByteBuffer grow() {
		return grow(0);
	}

	public ByteBuffer grow(int minCapacity) {
		var b = ByteBuffer.allocate(Math.max(buffer.capacity() * 2, minCapacity));
		buffer.flip();
		try {
			b.put(buffer);
		} finally {
			buffer.compact();
		}
		buffer = b;
		return buffer;
	}

	public ByteBuffer put(ByteBuffer src) {
		var o = src.remaining() - buffer.remaining();
		if (o > 0)
			grow(buffer.capacity() + o);
		return buffer.put(src);
	}
}
