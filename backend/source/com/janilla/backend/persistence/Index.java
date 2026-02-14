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
package com.janilla.backend.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.janilla.java.Java;
import com.janilla.java.Property;
import com.janilla.java.Reflection;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Index {

	Class<? extends BiFunction<Class<?>, String, Function<?, Set<List<Object>>>>> keyGetter() default Keyword.class;

	public static class Keyword implements BiFunction<Class<?>, String, Function<?, Set<List<Object>>>> {

		@Override
		public Function<?, Set<List<Object>>> apply(Class<?> class1, String name) {
			class A {
				private static final Map<Class<?>, Map<String, Function<?, Set<List<Object>>>>> RESULTS = new ConcurrentHashMap<>();
			}
			return A.RESULTS.computeIfAbsent(class1, _ -> new ConcurrentHashMap<>()).computeIfAbsent(name, _ -> {
				var p1 = Reflection.property(class1, name);
				var p2 = !p1.type().getPackageName().startsWith("java.") ? Reflection.property(p1.type(), "id") : null;
				var p = p2 != null ? Property.of(p1, p2) : p1;
				return x -> {
					var o = p.get(x);
					if (o != null)
						switch (o) {
						case Collection<?> oo:
							return oo.stream().map(y -> Java.arrayList((Object) y))
									.collect(Collectors.toCollection(LinkedHashSet::new));
						default:
							return Java.hashSet(Java.arrayList(o));
						}
					else
						return Set.of();
				};
			});
		}
	}

//	public static class KeywordSet extends Keyword {
//
//		public static Pattern space = Pattern.compile("\\s+");
//
//		@Override
//		public Function<Object, Object> apply(Class<?> t, String u) {
//			var f = super.apply(t, u);
//			return o -> {
//				var s = (String) f.apply(o);
//				if (s == null)
//					return Collections.emptyList();
//				var l = space.splitAsStream(s.toLowerCase()).distinct().sorted().toList();
//				return l;
//			};
//		}
//	}
}
