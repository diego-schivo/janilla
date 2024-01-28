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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Interpolator {

	public static void main(String[] args) throws IOException {
		record R(String s) {
		}
		var e = new Evaluator();
		e.setContext(Map.of("s", "foo", "r", new R("bar")));
		var i = new Interpolator();
		i.setEvaluators(Map.of('$', e));

		var j = new ByteArrayInputStream("""
				foo bar
				baz ${s} ${r.s} qux""".getBytes());
		var o = new ByteArrayOutputStream();
		try (var r = new BufferedReader(new InputStreamReader(j)); var w = new PrintWriter(o)) {
			r.lines().forEach(l -> i.print(l, w));
		}

		var s = o.toString();
		System.out.println(s);
		assert Objects.equals(s, """
				foo bar
				baz foo bar qux
				""") : s;
	}

	protected Map<Character, Function<String, Object>> evaluators;

	Supplier<String[]> prefixes = Lazy
			.of(() -> evaluators.keySet().stream().map(c -> String.valueOf(c) + "{").toArray(String[]::new));

	public Map<Character, Function<String, Object>> getEvaluators() {
		return evaluators;
	}

	public void setEvaluators(Map<Character, Function<String, Object>> evaluators) {
		this.evaluators = evaluators;
	}

	public void print(String template, PrintWriter writer) {
		var i = indexOf(template, prefixes.get());
		if (i < 0) {
//			writer.println(template);
			writer.print(template);
			return;
		}
		var j = -1;
		do {
			writer.print(template.substring(j + 1, i));
			j = template.indexOf('}', i + 2);
			var e = template.substring(i + 2, j);
			var o = evaluators.get(template.charAt(i)).apply(e);
			printObject(o, writer);
			i = indexOf(template, prefixes.get(), j + 1);
		} while (i >= 0);
//		writer.println(template.substring(j + 1));
		writer.print(template.substring(j + 1));
	}

	protected void printObject(Object object, PrintWriter writer) {
		writer.print(object);
	}

	static int indexOf(String string, String... substrings) {
		var i = -1;
		for (var s : substrings) {
			var j = string.indexOf(s);
			if (j >= 0 && (i < 0 || j < i))
				i = j;
		}
		return i;
	}

	static int indexOf(String string, String[] substrings, int from) {
		var i = -1;
		for (var s : substrings) {
			var j = string.indexOf(s, from);
			if (j >= 0 && (i < 0 || j < i))
				i = j;
		}
		return i;
	}
}
