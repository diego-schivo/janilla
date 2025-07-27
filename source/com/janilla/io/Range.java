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

import java.io.IO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.json.Converter;
import com.janilla.json.Json;

public record Range(long from, long to) {

	public static void main(String[] args) {
		var l = 50;

		var c = new Converter(null);
		var in = Stream.of(
//				"""
//						[{"from":23,"to":31},{"from":18,"to":50},{"from":28,"to":32},{"from":33,"to":44},{"from":41,"to":45}]""",
//				"""
//						[{"from":14,"to":18},{"from":13,"to":45},{"from":48,"to":49},{"from":32,"to":34},{"from":44,"to":47}]""",
//				"""
//						[{"from":13,"to":45},{"from":17,"to":35},{"from":22,"to":27},{"from":22,"to":30},{"from":13,"to":47}]""",
//				"""
//						[{"from":26,"to":36},{"from":19,"to":29},{"from":31,"to":36},{"from":13,"to":18},{"from":12,"to":40}]""",
//				"""
//						[{"from":18,"to":29},{"from":15,"to":27},{"from":41,"to":44},{"from":39,"to":42},{"from":12,"to":23}]""",
//				"""
//						[{"from":1,"to":8},{"from":11,"to":13},{"from":41,"to":46},{"from":8,"to":44},{"from":29,"to":44}]""",
				"""
						[]""")
				.map(x -> ((List<?>) Json.parse(x)).stream().map(y -> (Range) c.convert(y, Range.class)).toList())
				.collect(Collectors.toCollection(ArrayList::new));

		if (in.get(in.size() - 1).isEmpty()) {
			var tlr = ThreadLocalRandom.current();
			var irr = new ArrayList<Range>();
			for (var i = 0; i < 5; i++) {
				var f = tlr.nextInt(l);
				var t = tlr.nextInt(f, l) + 1;
				irr.add(new Range(f, t));
			}
			in.set(in.size() - 1, irr);
		}

		for (var irr : in) {
			IO.println("irr=" + Json.format(irr, true));

			var orr = new ArrayList<Range>();
			var bb = new boolean[l];
			irr.stream().forEach(x -> {
				IO.println("x=" + x);
				var rr = union(orr, x);
				IO.println("rr=" + rr.toList());
				IO.println("orr=" + orr);
				for (var j = (int) x.from; j < (int) x.to; j++)
					bb[j] = true;
			});

			var f = -1;
			var orr2 = new ArrayList<Range>();
			for (int i = 0; i < bb.length; i++) {
				if (f == -1) {
					if (bb[i])
						f = i;
				} else if (!bb[i]) {
					orr2.add(new Range(f, i));
					f = -1;
				}
			}
			if (f != -1)
				orr2.add(new Range(f, bb.length));
			assert orr2.equals(orr) : orr2;
		}
	}

	public static Stream<Range> union(List<Range> ranges, Range range) {
		var i = Collections.binarySearch(ranges, range, (x, y) -> Long.compare(x.from, y.from));
		i = i >= 0 ? i + 1 : -i - 1;

		var r1 = i > 0 ? ranges.get(i - 1) : null;
		if (r1 != null && r1.to < range.from)
			r1 = null;

		var b = Stream.<Range>builder();
		for (;;) {
			if (r1 == null) {
				var r2 = i < ranges.size() ? ranges.get(i) : null;
				if (r2 != null && r2.from <= range.to) {
					b.add(new Range(range.from, r2.from));
					r1 = new Range(range.from, r2.to);
					ranges.set(i, r1);
					if (r2.to < range.to) {
						range = new Range(r2.to, range.to);
						i++;
						continue;
					}
					break;
				}
			} else if (range.to <= r1.to)
				break;
			else if (range.from <= r1.to) {
				var r2 = i < ranges.size() ? ranges.get(i) : null;
				if (r2 != null && range.to < r2.from)
					r2 = null;
				b.add(new Range(r1.to, r2 != null ? Math.min(r2.from, range.to) : range.to));
				if (r2 != null)
					ranges.remove(i);
				if (r2 != null && r2.to < range.to) {
					r1 = new Range(r1.from, r2.to);
					continue;
				}
				ranges.set(i - 1, new Range(r1.from, r2 != null ? Math.max(r2.to, range.to) : range.to));
				break;
			}
			b.add(range);
			ranges.add(i, range);
			break;
		}
		return b.build();
	}

	public Range {
		if (to <= from)
			throw new IllegalArgumentException("to");
	}
}
