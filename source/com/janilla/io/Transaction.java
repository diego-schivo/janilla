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
package com.janilla.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Transaction {

	public static void main(String[] args) {
		var r = ThreadLocalRandom.current();
		var t = new Transaction(100);
		for (var i = 0; i < 3; i++) {
			var a = new Range(r.nextLong(100), r.nextLong(100));
			var b = t.include(a);
			System.out.println(a + "\t" + b);
		}
		System.out.println(t.ranges);
	}

	private long size;

	private List<Range> ranges = new ArrayList<>();

	public Transaction(long size) {
		this.size = size;
	}

	Range include(Range range) {
		if (range.start >= size || range.end <= range.start)
			return null;
		if (range.end > size)
			range = new Range(range.start, size);
		var i = Collections.binarySearch(ranges, range, (x, y) -> Long.compare(x.start, y.start));
		if (i < 0)
			i = -i - 1;

		var a = range;
		var g = i > 0 ? ranges.get(i - 1) : null;
		if (g != null && range.start <= g.end) {
			a = g.end < range.end ? new Range(g.start, range.end) : g;
			ranges.remove(--i);
			range = g.end < range.end ? new Range(g.end, range.end) : null;
		}
		g = range != null && i < ranges.size() ? ranges.get(i) : null;
		if (g != null && g.start <= range.end) {
			a = range.end < g.end ? new Range(range.start, g.end) : range;
			ranges.remove(i);
			range = range.start < g.start ? new Range(range.start, g.start) : null;
		}
		ranges.add(i, a);
		if (range == null)
			return null;
		if (range.end == size)
			return new Range(size, range.start);
		return range;
	}

	public record Range(long start, long end) {
	}
}
