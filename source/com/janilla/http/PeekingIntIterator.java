/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.http;

import java.util.PrimitiveIterator;

public class PeekingIntIterator implements PrimitiveIterator.OfInt {

	protected final PrimitiveIterator.OfInt iterator;

	protected boolean hasCurrent;

	protected int current;

	public PeekingIntIterator(PrimitiveIterator.OfInt iterator) {
		this.iterator = iterator;
	}

	public int peek() {
		return nextInt(true);
	}

	@Override
	public boolean hasNext() {
		return hasCurrent || iterator.hasNext();
	}

	@Override
	public int nextInt() {
		return nextInt(false);
	}

	protected int nextInt(boolean peek) {
		if (hasCurrent) {
			hasCurrent = peek;
			return current;
		}
		var i = iterator.nextInt();
		if (peek) {
			current = i;
			hasCurrent = true;
		}
		return i;
	}
}
