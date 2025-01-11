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
package com.janilla.json;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.reflect.Reflection;

public class ReflectionValueIterator extends ValueIterator {

	@Override
	protected Iterator<JsonToken<?>> newIterator() {
		var tt = super.newIterator();
		if (tt == null) {
			tt = switch (object) {
			case byte[] x -> context.newStringIterator(Base64.getEncoder().encodeToString(x));
			case long[] x -> context.newArrayIterator(Arrays.stream(x).boxed().iterator());
			case Object[] x -> context.newArrayIterator(Arrays.stream(x).iterator());
			case Collection<?> x -> context.newArrayIterator(x.iterator());
			case Stream<?> x -> context.newArrayIterator(x.iterator());
			default -> null;
			};
		}
		if (tt == null) {
			var c = Modifier.isPublic(object.getClass().getModifiers()) ? object.getClass() : switch (object) {
			case Map.Entry<?, ?> _ -> Map.Entry.class;
			default -> throw new RuntimeException();
			};
			if (c.isEnum())
				tt = context.newStringIterator(object.toString());
			else {
				var s = Reflection.properties(c).map(x -> {
//					System.out.println("ReflectionValueIterator.newIterator, x=" + x + ", object=" + object);
					Map.Entry<String, Object> y = new AbstractMap.SimpleEntry<>(x.name(), x.get(object));
					return y;
				});
				if (((ReflectionJsonIterator) context).includeType)
					s = Stream.concat(
							Stream.of(Map.entry("$type",
									(Object) c.getName().substring(c.getPackageName().length() + 1).replace('$', '.'))),
							s);
				tt = context.newObjectIterator(s.iterator());
			}
		}
		return tt;
	}
}
