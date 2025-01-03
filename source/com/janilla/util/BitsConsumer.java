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
package com.janilla.util;

import java.util.function.IntConsumer;

public class BitsConsumer implements IntConsumer {

	IntConsumer bytes;

	long current;

	int currentLength;
	
	int length;

	public BitsConsumer(IntConsumer bytes) {
		this.bytes = bytes;
	}
	
	public int length() {
		return length;
	}

	@Override
	public void accept(int value) {
		accept(value, 8);
	}

	public void accept(int value, int bitsLength) {
		current = (current << bitsLength) | (value & ((1L << bitsLength) - 1));
		var l = currentLength + bitsLength;
		for (; l >= 8; l -= 8) {
			bytes.accept((int) (current >>> (l - 8)));
			length++;
			current &= (1 << (l - 8)) - 1;
		}
		currentLength = l;
	}

	public void accept(byte[] bytes) {
		for (var b : bytes)
			accept(b);
	}
}
