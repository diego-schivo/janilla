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
package com.janilla.java;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DollarTypeResolver implements TypeResolver {

	protected final Map<String, Class<?>> parseMap;

	public DollarTypeResolver(List<Class<?>> resolvables) { // , DiFactory diFactory) {
		parseMap = resolvables.stream().collect(Collectors.toMap(this::format, x -> {
//			var c = (Class<?>) diFactory.classFor(x);
//			return c != null ? c : x;
			return x;
		}, (_, x) -> x));
//		IO.println("DollarTypeResolver, parseMap=" + parseMap);
	}

	@Override
	public TypedData apply(TypedData typedData) {
		var o = typedData.data() instanceof Map<?, ?> x ? x.get("$type") : null;
		var t = o instanceof String x ? parse(x) : null;
//		IO.println("o=" + o + ", t=" + t);
		return t != null ? typedData.withType(t) : null;
	}

	@Override
	public Class<?> parse(String string) {
		var c = parseMap.get(string);
//		IO.println("DollarTypeResolver.parse, string=" + string + ", c=" + c);
		return c;
	}

	@Override
	public String format(Class<?> type) {
		var s = type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
//		IO.println("DollarTypeResolver.format, type=" + type + ", s=" + s);
		return s;
	}
}
