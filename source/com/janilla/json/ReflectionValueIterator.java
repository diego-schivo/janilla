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
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.reflect.Reflection;

public class ReflectionValueIterator extends ValueIterator {

	@Override
	protected Iterator<JsonToken<?>> createIterator() {
		var i = super.createIterator();
		if (i == null) {
			i = switch (object) {
			case byte[] x -> context.buildStringIterator(Base64.getEncoder().encodeToString(x));
			case long[] x -> context.buildArrayIterator(Arrays.stream(x).boxed().iterator());
			case Object[] x -> context.buildArrayIterator(Arrays.stream(x).iterator());
			case Collection<?> x -> context.buildArrayIterator(x.iterator());
			case Stream<?> x -> context.buildArrayIterator(x.iterator());
			default -> null;
			};
		}
		if (i == null) {
			var c = Modifier.isPublic(object.getClass().getModifiers()) ? object.getClass() : switch (object) {
			case Map.Entry<?, ?> x -> Map.Entry.class;
			default -> throw new RuntimeException();
			};
			if (c.isEnum())
				i = context.buildStringIterator(object.toString());
			else {
				var s = Reflection.propertyNames(c).map(p -> {
					var g = Reflection.property(c, p);
					return g != null ? Map.entry(p, g) : null;
				}).filter(Objects::nonNull).map(e -> {
//				System.out.println(e.getValue() + " " + object);
					var v = e.getValue().get(object);
					Map.Entry<String, Object> f = new AbstractMap.SimpleEntry<>(e.getKey(), v);
					return f;
				});
				if (((ReflectionJsonIterator) context).includeType)
					s = Stream.concat(
							Stream.of(Map.entry("$type",
									(Object) c.getName().substring(c.getPackageName().length() + 1).replace('$', '.'))),
							s);
				i = context.buildObjectIterator(s.iterator());
			}
		}
		return i;
	}
}
