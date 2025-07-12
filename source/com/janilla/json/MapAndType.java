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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public record MapAndType(Map<?, ?> map, Class<?> type) {

	public interface TypeResolver extends UnaryOperator<MapAndType> {

		Class<?> parse(String string);

		String format(Class<?> class1);
	}

	public static class NullTypeResolver implements TypeResolver {

		@Override
		public MapAndType apply(MapAndType t) {
			return null;
		}

		@Override
		public Class<?> parse(String string) {
			return null;
		}

		@Override
		public String format(Class<?> class1) {
			return null;
		}
	}

	public static class DollarTypeResolver implements TypeResolver {

		protected final Set<Class<?>> types;

		protected final Map<String, Class<?>> resolveMap = new ConcurrentHashMap<>();

		public DollarTypeResolver(Set<Class<?>> types) {
			this.types = types;
		}

		@Override
		public MapAndType apply(MapAndType mt) {
			var c = parse((String) mt.map().get("$type"));
			return c != null ? new MapAndType(mt.map(), c) : null;
		}

		@Override
		public Class<?> parse(String t) {
//			System.out.println("TypeResolver.parse, t = " + t);
			if (t == null)
				return null;
			var c = resolveMap.computeIfAbsent(t, k -> {
				for (var x : types) {
					var n = x.getName().substring(x.getPackageName().length() + 1).replace('$', '.');
//					System.out.println("TypeResolver.parse, n = " + n);
					if (n.equals(k))
						return x;
				}
				return null;
			});
//			System.out.println("TypeResolver.parse, c = " + c);
			return c;
		}

		@Override
		public String format(Class<?> class1) {
			return class1.getName().substring(class1.getPackageName().length() + 1);
		}
	}
}
