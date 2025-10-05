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
package com.janilla.sqlite;

import java.nio.ByteBuffer;

public class Varint {

	public static long get(ByteBuffer buffer) {
		return get(buffer, -1);
	}

	public static long get(ByteBuffer buffer, int index) {
		var l = 0l;
		for (var i = 0; i < 9; i++) {
			var x = index == -1 ? Byte.toUnsignedInt(buffer.get()) : Byte.toUnsignedInt(buffer.get(index + i));
			var y = x & 0x7f;
			l = (l << 7) | y;
			if (x == y)
				return l;
		}
		throw new RuntimeException();
	}

	public static void put(ByteBuffer buffer, long value) {
		put(buffer, -1, value);
	}

	public static void put(ByteBuffer buffer, int index, long value) {
		var bb = bytes(value);
		if (index == -1)
			buffer.put(bb);
		else
			buffer.put(index, bb);
	}

	public static int size(long value) {
		for (var i = 0; i < 9; i++) {
			value >>>= 7;
			if (value == 0)
				return i + 1;
		}
		throw new RuntimeException();
	}

	public static int size(ByteBuffer buffer) {
		return size(buffer, -1);
	}

	public static int size(ByteBuffer buffer, int index) {
		for (var i = 0; i < 9; i++) {
			var x = index == -1 ? Byte.toUnsignedInt(buffer.get()) : Byte.toUnsignedInt(buffer.get(index + i));
			if ((x & 0x80) == 0)
				return i + 1;
		}
		throw new RuntimeException();
	}

	public static byte[] bytes(long value) {
		var bb = new byte[size(value)];
		for (var i = 0; i < bb.length; i++) {
			var x = (int) (value & 0x7f);
			value >>>= 7;
			if (i != 0)
				x |= 0x80;
			bb[bb.length - 1 - i] = (byte) x;
		}
		return bb;
	}
}
