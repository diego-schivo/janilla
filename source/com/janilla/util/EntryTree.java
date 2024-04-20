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
package com.janilla.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.janilla.json.Json;
import com.janilla.reflect.Reflection;

public class EntryTree {

	Map<String, Object> tree = new LinkedHashMap<String, Object>();

	Function<String, Class<?>> typeResolver;

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
	}

	public void add(Map.Entry<String, String> t) {
		Map<String, Object> n = tree;
		var k = t.getKey().split("\\.");
		for (var i = 0; i < k.length; i++) {
			if (k[i].endsWith("]")) {
				var l = k[i].split("\\[", 2);
				var j = Integer.parseInt(l[1].substring(0, l[1].length() - 1));
				@SuppressWarnings("unchecked")
				var a = (List<Object>) n.computeIfAbsent(l[0], x -> new ArrayList<Object>());
				while (a.size() <= j)
					a.add(null);
				if (i < k.length - 1) {
					@SuppressWarnings("unchecked")
					var o = (Map<String, Object>) a.get(j);
					if (o == null) {
						o = new LinkedHashMap<String, Object>();
						a.set(j, o);
					}
					n = o;
				} else
					a.set(j, t.getValue());
			} else if (i < k.length - 1) {
				@SuppressWarnings("unchecked")
				var o = (Map<String, Object>) n.computeIfAbsent(k[i], x -> new LinkedHashMap<String, Object>());
				n = o;
			} else
				n.put(k[i], t.getValue());
		}
	}

	public <T> T convert(Class<T> target) {
		return convert(tree, target);
	}

	protected <T> T convert(Map<String, Object> tree, Class<T> target) {
		if (tree.containsKey("$type"))
			try {
				@SuppressWarnings("unchecked")
				var c = (Class<T>) Class.forName(target.getPackageName() + "." + tree.get("$type"));
				if (!target.isAssignableFrom(c))
					throw new RuntimeException();
				target = c;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

		BiFunction<String, Type, Object> c = (name, type) -> {
			var i = tree.get(name);
			var o = Json.convert(i, type, typeResolver);
			return o;
		};

		try {
			if (target.isRecord()) {
				var a = new ArrayList<Object>();
				for (var d : target.getRecordComponents()) {
					var n = d.getName();
					var t = d.getGenericType();
					var b = c.apply(n, t);
					a.add(b);
				}
				@SuppressWarnings("unchecked")
				var t = (T) target.getConstructors()[0].newInstance(a.toArray());
				return t;
			}
			var z = target;
			var t = (T) z.getConstructor().newInstance();
			for (var i = Reflection.properties(z).map(n -> {
				var s = Reflection.property(z, n);
				return s != null ? Map.entry(n, s) : null;
			}).filter(Objects::nonNull).iterator();i.hasNext();) {
				var e = i.next();
				var n = e.getKey();
				var s = e.getValue();
				var u = s.getGenericType();
				var b = c.apply(n, u);
				if (b != null)
					s.set(t, b);
			}
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
