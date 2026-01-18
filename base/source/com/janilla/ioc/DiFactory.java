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
package com.janilla.ioc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.reflect.Reflection;

public class DiFactory {

	protected final List<Class<?>> types;

	protected final Supplier<Object> context;

	protected final String name;

	protected final Map<Class<?>, Function<Map<String, Object>, ?>> factories = new ConcurrentHashMap<>();

	public DiFactory(List<Class<?>> types, Supplier<Object> context) {
		this(types, context, null);
	}

	public DiFactory(List<Class<?>> types, Supplier<Object> context, String name) {
//		IO.println("Factory, types=" + types);
		this.types = types;
		this.context = context;
		this.name = name;
	}

	public List<Class<?>> types() {
		return types;
	}

	public Object context() {
		return context.get();
	}

	public <T> T create(Class<T> type) {
		return create(type, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments) {
//		IO.println("Factory.create, type=" + type + ", arguments=" + arguments);
		var f = factories.computeIfAbsent(type, k -> {
			var c = Stream.concat(types.stream(), Stream.of(k)).filter(x -> !Modifier.isAbstract(x.getModifiers()))
					.filter(k::isAssignableFrom).filter(x -> {
						var a = x.getAnnotation(Context.class);
						var nn = a != null ? a.value() : null;
						return nn == null || Arrays.stream(nn).anyMatch(y -> y.equals(name));
					}).findFirst().orElse(null);
//			IO.println("Factory.create, c=" + c);
			if (c == null)
//				throw new RuntimeException("type=" + type);
				return _ -> null;
			var cc = c.getConstructors();
			var o = context.get();
			var oe = o != null && c.getEnclosingClass() == o.getClass() && !Modifier.isStatic(c.getModifiers());
			return aa -> {
				try {
					Constructor<?> c1 = null;
					Object[] aa1 = null;
					var n1 = -1;
					for (var c2 : cc) {
						var pp = Arrays.stream(c2.getParameters());
						var aa2 = (oe ? pp.skip(1) : pp).map(x -> {
							if (aa != null && aa.containsKey(x.getName()))
								return aa.get(x.getName());
							var p = o != null ? Reflection.property(o.getClass(), x.getName()) : null;
							return p != null ? p.get(o) : null;
						}).toArray();
						var n2 = (int) Arrays.stream(aa2).filter(Objects::nonNull).count();
						if (n2 > n1) {
							c1 = c2;
							aa1 = aa2;
							n1 = n2;
						}
					}
//					IO.println("Factory.create, c1 = " + c1);
					if (oe) {
						var oo = new Object[1 + aa1.length];
						oo[0] = o;
						System.arraycopy(aa1, 0, oo, 1, aa1.length);
						aa1 = oo;
					}
					@SuppressWarnings("unchecked")
					var t = (T) c1.newInstance(aa1);
					if (o != null)
						t = Reflection.copy(o, t);
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

	public static void main(String[] args) {
		var z = new Foo("a");
		var f = new DiFactory(List.of(Foo.C.class), () -> z);
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
}
