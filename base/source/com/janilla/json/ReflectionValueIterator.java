/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.java.Property;

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
			case double[] x -> context.newArrayIterator(Arrays.stream(x).boxed());
			case int[] x -> context.newArrayIterator(Arrays.stream(x).boxed());
			case long[] x -> context.newArrayIterator(Arrays.stream(x).boxed());
			case Object[] x -> context.newArrayIterator(Arrays.stream(x));
			case Collection<?> x -> context.newArrayIterator(x.stream());
			case Stream<?> x -> context.newArrayIterator(x);
			default -> null;
			};
			if (tt == null) {
				var c = value instanceof Map.Entry ? Map.Entry.class : value.getClass();
				tt = context.newObjectIterator(entries(c));
			}
		}
		return tt;
	}

	protected Stream<Map.Entry<String, Object>> entries(Class<?> type) {
//		IO.println("ReflectionValueIterator.entries, type=" + type);
		var ee = type.isEnum() ? Stream.of(Map.entry("name", (Object) ((Enum<?>) value).name()))
				: JavaReflect.properties(type).filter(this::includeEntry).map(x -> {
//					IO.println("ReflectionValueIterator.entries, x=" + x + ", value=" + value);
					return (Map.Entry<String, Object>) Java.mapEntry(x.name(), x.get(value));
				});
//		if (((ReflectionJsonIterator) context).includeType) 
		if (((ReflectionJsonIterator) context).typeResolver != null) {
			var t = Modifier.isPublic(type.getModifiers()) ? type : type.getInterfaces()[0];
			ee = Stream.concat(Stream.of(Map.entry("$type", t)), ee);
		}

		{
			var l = ee.toList();
//			IO.println("ReflectionValueIterator.entries, l=" + l);
			ee = l.stream();
		}

		return ee;
	}

	protected boolean includeEntry(Property property) {
		return property.canGet();
	}

	@Override
	protected String toString(Class<?> type) {
		var r = ((ReflectionJsonIterator) context).typeResolver;
		return r != null ? r.format(type) : super.toString(type);
	}
}
