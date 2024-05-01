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
package com.janilla.json;

import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.reflect.Reflection;

class ReflectionValueIterator extends ValueIterator {

	ReflectionValueIterator(Object object, TokenIterationContext context) {
		super(object, context);
	}

	@Override
	protected Iterator<JsonToken<?>> newIterator(Object object) {
		var i = super.newIterator(object);
		if (i == null) {
			i = switch (object) {
			case byte[] x -> new StringIterator(Base64.getEncoder().encodeToString(x));
			case long[] x -> new ArrayIterator(Arrays.stream(x).boxed().iterator(), context);
			case Object[] x -> new ArrayIterator(Arrays.stream(x).iterator(), context);
			case Collection<?> x -> new ArrayIterator(x.iterator(), context);
			case Stream<?> x -> new ArrayIterator(x.iterator(), context);
			default -> null;
			};
		}
		if (i == null) {
			var c = Modifier.isPublic(object.getClass().getModifiers()) ? object.getClass() : switch (object) {
			case Map.Entry<?, ?> x -> Map.Entry.class;
			default -> throw new RuntimeException();
			};
			var s1 = Stream.of(Map.entry("$type",
					(Object) c.getName().substring(c.getPackageName().length() + 1).replace('$', '.')));
			var s2 = Reflection.properties(c).map(p -> {
				var g = Reflection.property(c, p);
//				var s = Reflection.property(c, p);
//				return g != null && s != null ? Map.entry(p, g) : null;
				return g != null ? Map.entry(p, g) : null;
			}).filter(Objects::nonNull).map(e -> {
//					System.out.println(e.getValue() + " " + object);
				var v = e.getValue().get(object);
				Map.Entry<String, Object> f = new SimpleEntry<>(e.getKey(), v);
				return f;
			});
			i = new ObjectIterator(Stream.concat(s1, s2).iterator(), context);
		}
		return i;
	}
}
