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
package com.janilla.net;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class AmountLimiter {

	private static final Logger LOGGER = System.getLogger(AmountLimiter.class.getName());

	protected final long limit;

	protected volatile long total;

	public AmountLimiter(long limit) {
		if (limit <= 0)
			throw new IllegalArgumentException("limit=" + limit);

		this.limit = limit;
	}

	public synchronized boolean test(int number) {
		LOGGER.log(Level.DEBUG, "number={0}", number);

		if (number < 0)
			throw new IllegalArgumentException("number=" + number);

		var g = total > limit;

		if (!g) {
			total += number;
			g = total > limit;
		}

		return !g;
	}

	public synchronized void reset() {
		total = 0;
	}
}
