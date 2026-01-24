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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.java.Reflection;

public class DiFactory {

	protected final List<Class<?>> types;

	protected Object context;

	protected final String name;

	protected final Map<Class<?>, Class<?>> actualTypes = new HashMap<>();

	protected final Map<Class<?>, Function<Map<String, Object>, ?>> factories = new ConcurrentHashMap<>();

	public DiFactory(List<Class<?>> types) {
		this(types, null);
	}

	public DiFactory(List<Class<?>> types, String name) {
//		IO.println("DiFactory, types=" + types);
		this.types = types;
		this.name = name;
	}

	public List<Class<?>> types() {
		return types;
	}

	public Object context() {
		return context;
	}

	public DiFactory context(Object context) {
		if (this.context != null)
			throw new IllegalStateException();
		this.context = context;
		return this;
	}

	public <T, U extends T> Class<U> actualType(Class<T> type) {
		if (!actualTypes.containsKey(type))
			synchronized (actualTypes) {
				if (!actualTypes.containsKey(type))
					actualTypes.put(type,
							Stream.concat(Stream.of(type), types.stream())
									.filter(x -> !Modifier.isAbstract(x.getModifiers())).filter(type::isAssignableFrom)
									.filter(x -> {
										var a = x.getAnnotation(Context.class);
										var nn = a != null ? a.value() : null;
										return nn == null || Arrays.stream(nn).anyMatch(y -> y.equals(name));
									}).reduce((_, x) -> x).orElse(null));
			}
		@SuppressWarnings("unchecked")
		Class<U> x = (Class<U>) actualTypes.get(type);
		return x;
	}

	public <T> T create(Class<T> type) {
		return create(type, null);
	}

	public <T> T create(Class<T> type, Map<String, Object> arguments) {
//		IO.println("DiFactory.create, type=" + type + ", arguments=" + arguments);
		var f = factories.computeIfAbsent(type, k -> {
			var t = actualType(k);
//			IO.println("DiFactory.create, c=" + c);
			if (t == null)
//				throw new RuntimeException("type=" + type);
				return _ -> null;
			var cc = t.getConstructors();
			var o = context;// .get();
			var oe = !Modifier.isStatic(t.getModifiers()) && o != null && t.getEnclosingClass() == o.getClass();
			return aa -> newInstance(cc, aa, o, oe);
		});
		@SuppressWarnings("unchecked")
		var t = (T) f.apply(arguments);
		return t;
	}

	protected <T> T newInstance(Constructor<?>[] constructors, Map<String, Object> arguments, Object context,
			boolean enclosed) {
		record R(Constructor<?> c, Object[] aa, int n) {
		}
		try {
			var r = new R(null, null, -1);
			for (var c : constructors) {
				var pp = Arrays.stream(c.getParameters());
				var aa = (enclosed ? pp.skip(1) : pp).map(x -> {
					if (arguments != null && arguments.containsKey(x.getName()))
						return arguments.get(x.getName());
					var p = context != null ? Reflection.property(context.getClass(), x.getName()) : null;
					return p != null ? p.get(context) : null;
				}).toArray();
				var n = (int) Arrays.stream(aa).filter(Objects::nonNull).count();
				if (n > r.n || (n == r.n && c.getParameterCount() < r.c.getParameterCount()))
					r = new R(c, aa, n);
			}
//			IO.println("DiFactory.create, c1 = " + c1);
			if (enclosed) {
				var aa = new Object[1 + r.aa.length];
				aa[0] = context;
				System.arraycopy(r.aa, 0, aa, 1, r.aa.length);
				r = new R(r.c, aa, r.n);
			}
//			IO.println("r=" + r);
			@SuppressWarnings("unchecked")
			var t = (T) r.c.newInstance(r.aa);
//			if (context != null)
//				t = Reflection.copy(context, t);
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

//	public static void main(String[] args) {
//		var z = new Foo("a");
//		var f = new DiFactory(List.of(Foo.C.class), () -> z);
//		var x1 = f.create(Foo.I.class, Map.of("s2", "b"));
//		IO.println("x1=" + x1);
//		var x2 = f.create(Foo.I.class, Map.of("s2", "c"));
//		IO.println("x2=" + x2);
//	}
//
//	public static class Foo {
//
//		public final String s1;
//
//		public Foo(String s1) {
//			this.s1 = s1;
//		}
//
//		public interface I {
//		}
//
//		public class C implements I {
//
//			public String s1;
//
//			private final String s2;
//
//			public C(String s2) {
//				this.s2 = s2;
//			}
//
//			@Override
//			public String toString() {
//				return s1 + s2;
//			}
//		}
//	}
}
