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

//import java.io.IO;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;

public class ReflectionValueIterator extends ValueIterator {

	public ReflectionValueIterator(TokenIterationContext context, Object object) {
		super(context, object);
	}

	@Override
	protected Iterator<JsonToken<?>> newIterator() {
		var tt = super.newIterator();
		if (tt == null) {
			tt = switch (value) {
			case byte[] x -> context.newStringIterator(Base64.getEncoder().encodeToString(x));
			case double[] x -> context.newArrayIterator(Arrays.stream(x).boxed().iterator());
			case int[] x -> context.newArrayIterator(Arrays.stream(x).boxed().iterator());
			case long[] x -> context.newArrayIterator(Arrays.stream(x).boxed().iterator());
			case Object[] x -> context.newArrayIterator(Arrays.stream(x).iterator());
			case Collection<?> x -> context.newArrayIterator(x.iterator());
			case Stream<?> x -> context.newArrayIterator(x.iterator());
			default -> null;
			};
			if (tt == null) {
				var c = Modifier.isPublic(value.getClass().getModifiers()) ? value.getClass() : switch (value) {
				case Map.Entry<?, ?> _ -> Map.Entry.class;
				default -> throw new RuntimeException();
				};
				tt = context.newObjectIterator(entries(c).iterator());
			}
		}
		return tt;
	}

	protected Stream<Map.Entry<String, Object>> entries(Class<?> type) {
//		IO.println("ReflectionValueIterator.entries, type=" + type);
		var kkvv = type.isEnum() ? Stream.of(Map.<String, Object>entry("name", ((Enum<?>) value).name()))
				: Reflection.properties(type).filter(Property::canGet).map(x -> {
//					IO.println("ReflectionValueIterator.entries, x=" + x + ", value=" + value);
					return (Map.Entry<String, Object>) new AbstractMap.SimpleImmutableEntry<>(x.name(), x.get(value));
				});
		if (((ReflectionJsonIterator) context).includeType) {
			var x = (Object) type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
			kkvv = Stream.concat(Stream.of(Map.entry("$type", x)), kkvv);
		}
		return kkvv;
	}
}
