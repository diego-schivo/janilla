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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Randomize {

	static String word(int minLength, int maxLength) {
		var r = ThreadLocalRandom.current();
		var s = IntStream.range(0, r.nextInt(minLength, maxLength + 1))
				.mapToObj(i -> Character.toString(r.nextInt('a', 'z' + 1))).collect(Collectors.joining());
		return s;
	}

	static String phrase(int minCount, int maxCount, Supplier<String> word) {
		return elements(minCount, maxCount, word).collect(Collectors.joining(" "));
	}

	static String sentence(int minCount, int maxCount, Supplier<String> word) {
		return Util.capitalizeFirstChar(phrase(minCount, maxCount, word)) + ".";
	}

	static <E> Stream<E> elements(int minCount, int maxCount, Supplier<E> element) {
		var r = ThreadLocalRandom.current();
		var s = IntStream.range(0, r.nextInt(minCount, maxCount + 1)).mapToObj(i -> element.get());
		return s;
	}

	static <E> Stream<E> elements(int minCount, int maxCount, List<E> list) {
		var r = ThreadLocalRandom.current();
		return r.ints(r.nextInt(minCount, maxCount + 1), 0, list.size()).mapToObj(list::get);
	}

	static <E> E element(List<E> list) {
		var r = ThreadLocalRandom.current();
		return list.get(r.nextInt(0, list.size()));
	}

	static Instant instant(Instant origin, Instant bound) {
		var r = ThreadLocalRandom.current();
		var m = r.nextLong(origin.toEpochMilli(), bound.toEpochMilli());
		var i = Instant.ofEpochMilli(m);
		return i;
	}
}
