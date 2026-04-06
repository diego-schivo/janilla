/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.conduit.backend;

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
				.mapToObj(_ -> Character.toString(r.nextInt('a', 'z' + 1))).collect(Collectors.joining());
		return s;
	}

	static String phrase(int minCount, int maxCount, Supplier<String> word) {
		return elements(minCount, maxCount, word).collect(Collectors.joining(" "));
	}

	static String sentence(int minCount, int maxCount, Supplier<String> word) {
		return capitalizeFirstChar(phrase(minCount, maxCount, word)) + ".";
	}

	static <E> Stream<E> elements(int minCount, int maxCount, Supplier<E> element) {
		var r = ThreadLocalRandom.current();
		var s = IntStream.range(0, r.nextInt(minCount, maxCount + 1)).mapToObj(_ -> element.get());
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

	static String capitalizeFirstChar(String string) {
		return string != null && !string.isEmpty() && !Character.isUpperCase(string.charAt(0))
				? Character.toString(Character.toUpperCase(string.charAt(0))) + string.substring(1)
				: string;
	}
}
