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
package com.janilla.util;

import java.nio.ByteBuffer;
import java.util.PrimitiveIterator;

import com.janilla.io.IO;

public class BitsIterator implements PrimitiveIterator.OfInt {

	public static BitsIterator of(ByteBuffer buffer) {
		return new BitsIterator(IO.toIntStream(buffer).iterator());
	}

	PrimitiveIterator.OfInt bytes;

	long current;

	int currentLength;

	public BitsIterator(PrimitiveIterator.OfInt bytes) {
		this.bytes = bytes;
	}

	@Override
	public boolean hasNext() {
		return hasNext(8);
	}

	@Override
	public int nextInt() {
		return nextInt(8);
	}

	public boolean hasNext(int bitLength) {
		return currentLength >= bitLength || bytes.hasNext();
	}

	public int nextInt(int bitLength) {
		while (currentLength < bitLength) {
			var i = bytes.nextInt();
			current = (current << 8) | i;
			currentLength += 8;
		}
		var l = currentLength - bitLength;
		var i = (int) (current >>> l);
		current &= (1 << l) - 1;
		currentLength = l;
		return i;
	}
}
