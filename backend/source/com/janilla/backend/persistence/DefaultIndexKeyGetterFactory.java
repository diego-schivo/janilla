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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.java.Property;
import com.janilla.java.Reflection;

public class DefaultIndexKeyGetterFactory implements IndexKeyGetterFactory {

	@Override
	public IndexKeyGetter keyGetter(Class<?> type, String... names) {
//		IO.println("DefaultIndexKeyGetterFactory.keyGetter, type=" + type + ", names=" + Arrays.toString(names));
		class A {
			private static final Map<Class<?>, Map<List<String>, IndexKeyGetter>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(type, _ -> new ConcurrentHashMap<>()).computeIfAbsent(List.of(names),
				_ -> new DefaultIndexKeyGetter(Arrays.stream(names).map(x -> {
					var p1 = Reflection.property(type, x);
					var p2 = !p1.type().getPackageName().startsWith("java.") ? Reflection.property(p1.type(), "id")
							: null;
					return p2 != null ? Property.of(p1, p2) : p1;
				}).toArray(Property[]::new)));
	}
}
