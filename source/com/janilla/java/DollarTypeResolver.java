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
package com.janilla.java;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class DollarTypeResolver implements TypeResolver {

	protected final Map<String, Class<?>> parseMap;

	public DollarTypeResolver(Collection<Class<?>> types) {
		parseMap = types.stream().collect(Collectors.toMap(this::format, x -> x, (x, _) -> x));
	}

	@Override
	public TypedData apply(TypedData ot) {
		var o = ot.data() instanceof Map<?, ?> x ? x.get("$type") : null;
		var t = o instanceof String x ? parse(x) : null;
		return t != null ? ot.withType(t) : null;
	}

	@Override
	public Class<?> parse(String string) {
		var x = parseMap.get(string);
//		IO.println("DollarTypeResolver.parse, string=" + string + ", c=" + c);
		return x;
	}

	@Override
	public String format(Class<?> type) {
		var x = type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
//		IO.println("DollarTypeResolver.format, class1=" + class1 + ", x=" + x);
		return x;
	}
}
