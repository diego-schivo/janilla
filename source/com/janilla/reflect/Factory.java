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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class Factory {

	public static void main(String[] args) {
		var f = new Factory(List.of(Foo.C.class), new Foo("a"));
		var x1 = f.create(Foo.I.class, Map.of("s2", "b"));
		System.out.println("x1=" + x1);
		var x2 = f.create(Foo.I.class, Map.of("s2", "c"));
		System.out.println("x2=" + x2);
	}

	public static class Foo {

		public final String s1;

		public Foo(String s1) {
			this.s1 = s1;
		}

		public interface I {
		}

		public class C implements I {

			public String s1;

			private final String s2;

			public C(String s2) {
				this.s2 = s2;
			}

			@Override
			public String toString() {
				return s1 + s2;
			}
		}
	}

	protected final Collection<Class<?>> types;

	protected final Object source;

	protected final Map<Class<?>, Function<Map<String, Object>, ?>> functions = new ConcurrentHashMap<>();

	public Factory(Collection<Class<?>> types, Object source) {
		this.types = types;
		this.source = source;
	}

	public Collection<Class<?>> types() {
		return types;
	}

	public Object source() {
		return source;
	}

	public <T> T create(Class<T> type) {
		return create(type, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments) {
		var f = functions.computeIfAbsent(type, k -> {
			var c = k;
			for (var x : types)
				if (type.isAssignableFrom(x) && !Modifier.isAbstract(x.getModifiers())) {
					c = x;
					break;
				}
			var cc = c.getConstructors();
			if (cc.length != 1)
				throw new RuntimeException(c + " has " + cc.length + " constructors");
			var c0 = cc[0];
//			System.out.println("Factory.create, c0 = " + c0);
			if (c.getEnclosingClass() == source.getClass())
				return aa -> {
					try {
						var aa2 = Stream.concat(Stream.of(source), Arrays.stream(c0.getParameters()).skip(1).map(x -> {
							if (aa != null && aa.containsKey(x.getName()))
								return aa.get(x.getName());
							var p = Reflection.property(source.getClass(), x.getName());
							return p != null ? p.get(source) : null;
						})).toArray();
						@SuppressWarnings("unchecked")
						var t = (T) c0.newInstance(aa2);
						return Reflection.copy(source, t);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			return aa -> {
				try {
					var aa2 = Arrays.stream(c0.getParameters()).map(x -> {
						if (aa != null && aa.containsKey(x.getName()))
							return aa.get(x.getName());
						var p = Reflection.property(source.getClass(), x.getName());
						return p != null ? p.get(source) : null;
					}).toArray();
					@SuppressWarnings("unchecked")
					var t = (T) c0.newInstance(aa2);
					return Reflection.copy(source, t);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			};
		});
		@SuppressWarnings("unchecked")
		var t = (T) f.apply(arguments);
		return t;
	}
}
