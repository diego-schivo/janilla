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
package com.janilla.reflect;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Factory {

	private Collection<Class<?>> types;

	private Object source;

	private Map<Class<?>, Supplier<?>> suppliers = new ConcurrentHashMap<>();

	public Collection<Class<?>> getTypes() {
		return types;
	}

	public void setTypes(Collection<Class<?>> types) {
		this.types = types;
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public <T> T create(Class<T> type) {
		return create(type, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments) {
		@SuppressWarnings("unchecked")
		var s = (Supplier<T>) suppliers.computeIfAbsent(type, k -> {
			for (var t : types)
				if (type.isAssignableFrom(t) && !Modifier.isAbstract(t.getModifiers())) {
					k = t;
					break;
				}
			var c = k;
			var c0 = c.getConstructors()[0];
//			System.out.println("Factory.create, c0 = " + c0);
			if (c.getEnclosingClass() == source.getClass())
				return () -> {
					try {
						@SuppressWarnings("unchecked")
						var t = (T) c0.newInstance(source);
						return Reflection.copy(source, t);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			return () -> {
				try {
					@SuppressWarnings("unchecked")
					var t = (T) c0
							.newInstance(Arrays.stream(c0.getParameters())
									.map(x -> arguments.containsKey(x.getName()) ? arguments.get(x.getName())
											: Reflection.property(source.getClass(), x.getName()).get(source))
									.toArray());
					return Reflection.copy(source, t);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			};
		});
		return s.get();
	}
}
