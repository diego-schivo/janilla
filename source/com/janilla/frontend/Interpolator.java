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
package com.janilla.frontend;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.io.IO;

public record Interpolator(String[] tokens) implements IO.Function<IO.Function<Object, Object>, String> {

	public static IO.Function<IO.Function<Object, Object>, String> of(String template) {
		var b = Stream.<String>builder();
		var i1 = 0;
		for (;;) {
			var i2a = template.indexOf("${", i1);
			var i2b = template.indexOf("#{", i1);
			var i2 = i2a >= 0 && (i2b < 0 || i2a <= i2b) ? i2a : i2b;
			if (i2 < 0) {
				b.add(template.substring(i1));
				break;
			}
			b.add(template.substring(i1, i2));
			i1 = i2 + 2;
			i2 = template.indexOf("}", i1);
			b.add(template.substring(i1, i2));
			i1 = i2 + 1;
		}
		var t = b.build().toArray(String[]::new);
		return new Interpolator(t);
	}

	@Override
	public String apply(IO.Function<Object, Object> evaluator) throws IOException {
		var b = Stream.<String>builder();
		var e = false;
		for (var t : tokens) {
			b.add(e ? Objects.toString(evaluator.apply(t), "") : t);
			e = !e;
		}
		return b.build().collect(Collectors.joining(""));
	}
}
