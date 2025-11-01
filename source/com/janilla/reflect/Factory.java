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
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Factory {

	public static void main(String[] args) {
		var z = new Foo("a");
		var f = new Factory(List.of(Foo.C.class), () -> z);
		var x1 = f.create(Foo.I.class, Map.of("s2", "b"));
		IO.println("x1=" + x1);
		var x2 = f.create(Foo.I.class, Map.of("s2", "c"));
		IO.println("x2=" + x2);
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

	protected final Supplier<Object> source;

	protected final Map<Class<?>, List<Class<?>>> foo = new ConcurrentHashMap<>();

	protected final Map<Class<?>, Function<Map<String, Object>, ?>> functions = new ConcurrentHashMap<>();

	public Factory(Collection<Class<?>> types, Supplier<Object> source) {
//		IO.println("Factory, types=" + types);
		this.types = types;
		this.source = source;
	}

	public Collection<Class<?>> types() {
		return types;
	}

	public Object source() {
		return source.get();
	}

	public <T> T create(Class<T> type) {
		return create(type, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments) {
		return create(type, arguments, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments, Function<List<Class<?>>, Class<?>> bar) {
//		IO.println("Factory.create, type = " + type);
		var tt = Stream.concat(
				foo.computeIfAbsent(type, k -> types.stream().filter(x -> k.isAssignableFrom(x)).toList()).stream(),
				Stream.of(type)).toList();
		var t1 = bar != null ? bar.apply(tt) : tt.getFirst();
		var f = functions.computeIfAbsent(t1, k -> {
			var c = !Modifier.isAbstract(k.getModifiers()) ? k : null;
			for (var x : types)
				if (type.isAssignableFrom(x) && !Modifier.isAbstract(x.getModifiers())) {
					c = x;
					break;
				}
			if (c == null)
				return _ -> null;
			var cc = c.getConstructors();
			if (cc.length != 1)
				throw new RuntimeException(c + " has " + cc.length + " constructors");
			var c0 = cc[0];
//			IO.println("Factory.create, c0 = " + c0);
			var source1 = source.get();
			if (source1 != null && c.getEnclosingClass() == source1.getClass())
				return aa -> {
					try {
						var aa2 = Stream.concat(Stream.of(source1), Arrays.stream(c0.getParameters()).skip(1).map(x -> {
							if (aa != null && aa.containsKey(x.getName()))
								return aa.get(x.getName());
							var p = Reflection.property(source1.getClass(), x.getName());
							return p != null ? p.get(source1) : null;
						})).toArray();
						@SuppressWarnings("unchecked")
						var t = (T) c0.newInstance(aa2);
						return Reflection.copy(source1, t);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			return aa -> {
				try {
					var aa2 = Arrays.stream(c0.getParameters()).map(x -> {
						if (aa != null && aa.containsKey(x.getName()))
							return aa.get(x.getName());
						var p = source1 != null ? Reflection.property(source1.getClass(), x.getName()) : null;
						return p != null ? p.get(source1) : null;
					}).toArray();
					@SuppressWarnings("unchecked")
					var t = (T) c0.newInstance(aa2);
					if (source1 != null)
						t = Reflection.copy(source1, t);
					return t;
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
