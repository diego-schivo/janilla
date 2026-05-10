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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.java.Java;
import com.janilla.java.JavaInvoke;
import com.janilla.java.JavaReflect;

public class DefaultDiFactory implements DiFactory {

	protected final Map<Type, Optional<Class<?>>> classes = new ConcurrentHashMap<>();

	protected final Supplier<Object> context;

	protected final Map<Class<?>, Function<Map<String, Object>, ?>> factories = new ConcurrentHashMap<>();

	protected final String scope;

	protected final List<Class<?>> types;

	public DefaultDiFactory(List<Class<?>> types, Supplier<Object> context, String scope) {
//		IO.println("DefaultDiFactory, types=" + types + ", scope=" + scope);
		this.types = types;
		this.context = context;
		this.scope = scope;
	}

	@Override
	public Object context() {
		return context.get();
	}

	@Override
	public Stream<Class<?>> types() {
		return types.stream();
	}

	@Override
	public Class<?> classFor(Type type) {
//		IO.println("DefaultDiFactory.classFor, type=" + type);
		var c = classes.computeIfAbsent(type, k -> {
			var cc = k instanceof Class x ? Stream.concat(Stream.<Class<?>>of(x), types.stream()) : types.stream();
			return cc.filter(predicate(k)).reduce((_, x) -> x);
		}).orElse(null);
//		IO.println("DefaultDiFactory.classFor, c=" + c);
		return c;
	}

	protected Predicate<Class<?>> predicate(Type type) {
		Predicate<Class<?>> p = x -> !(x.isInterface() || Modifier.isAbstract(x.getModifiers())
				|| (x.isMemberClass() && !Modifier.isStatic(x.getModifiers())));

		p = p.and(Java.toClass(type)::isAssignableFrom);

		if (type instanceof ParameterizedType pt1) {
			var cc1 = Stream.concat(Stream.of(pt1.getRawType()), Arrays.stream(pt1.getActualTypeArguments()))
					.map(Java::toClass).toList();
//			IO.println("cc1=" + cc1);
			p = p.and(t -> JavaReflect.getAllActualInterfaces(t).filter(x -> x instanceof ParameterizedType)
					.anyMatch(x -> {
//						IO.println("x=" + x);
						var cc2 = x instanceof ParameterizedType pt2 ? Stream
								.concat(Stream.of(pt2.getRawType()), Arrays.stream(pt2.getActualTypeArguments()))
								.map(Java::toClass).toList() : null;
//						IO.println("cc2=" + cc2);
						return cc2 != null && IntStream.range(0, cc1.size())
								.allMatch(i -> cc2.get(i).isAssignableFrom(cc1.get(i)));
					}));
		}

		if (scope != null)
			p = p.and(t -> {
				var a = t.getAnnotation(Scope.class);
				var nn = a != null ? a.value() : null;
				return nn == null || Arrays.stream(nn).anyMatch(x -> x.equals(scope));
			});

		return p;
	}

	@Override
	public <T> T newInstance(Class<T> class1, Map<String, Object> arguments) {
//		IO.println("DefaultDiFactory.newInstance, class1=" + class1 + ", arguments=" + arguments);
		Objects.requireNonNull(class1);
		var f = factories.computeIfAbsent(class1, _ -> {
			var cc = !class1.isInterface() && !Modifier.isAbstract(class1.getModifiers())
					? Stream.of(class1.getConstructors(), class1.getDeclaredConstructors()).filter(x -> x.length != 0)
							.findFirst().orElse(null)
					: null;
			if (cc == null || cc.length == 0)
				throw new IllegalArgumentException("class1=" + class1);
			var o = context();
			var oe = !Modifier.isStatic(class1.getModifiers()) && o != null
					&& class1.getEnclosingClass() == o.getClass();
			return aa -> newInstance2(cc, aa, o, oe);
		});
		@SuppressWarnings("unchecked")
		var t = (T) f.apply(arguments);
		return t;
	}

	protected <T> T newInstance2(Constructor<?>[] constructors, Map<String, Object> arguments, Object context,
			boolean enclosed) {
//		IO.println("DefaultDiFactory.newInstance, constructors=" + Arrays.toString(constructors) + ", arguments=" + arguments
//				+ ", context=" + context + ", enclosed=" + enclosed);
		record R(Constructor<?> c, Object[] aa, boolean f, int n) {
		}
		var r = new R(null, null, false, -1);
		for (var c : constructors) {
			var pp = Arrays.stream(c.getParameters());
			var aa = (enclosed ? pp.skip(1) : pp).map(x -> {
				if (arguments != null && arguments.containsKey(x.getName()))
					return arguments.get(x.getName());
				var p = context != null ? JavaReflect.property(context.getClass(), x.getName()) : null;
				if (p != null)
					return p.get(context);
				return context != null && x.getType().isAssignableFrom(context.getClass()) ? context : null;
			}).toArray();
			var n = (int) Arrays.stream(aa).filter(Objects::nonNull).count();
			var f = Arrays.stream(aa).anyMatch(x -> x instanceof DiFactory);
			if (f != r.f ? f : n > r.n || (n == r.n && c.getParameterCount() < r.c.getParameterCount()))
				r = new R(c, aa, f, n);
		}
		if (enclosed) {
			var aa = new Object[1 + r.aa.length];
			aa[0] = context;
			System.arraycopy(r.aa, 0, aa, 1, r.aa.length);
			r = new R(r.c, aa, r.f, r.n);
		}
//		IO.println("DefaultDiFactory.newInstance, r=" + r);
		try {
			@SuppressWarnings("unchecked")
			var t = (T) JavaInvoke.methodHandle(r.c).invokeWithArguments(r.aa);
			return t;
		} catch (Throwable e) {
			throw e instanceof RuntimeException e2 ? e2 : new RuntimeException(e);
		}
	}

//	public static void main(String[] args) {
//		var z = new Foo("a");
//		var f = Ioc.newDiFactory(List.of(Foo.C.class), () -> z);
//		var x1 = f.create(f.actualType(Foo.I.class), Map.of("s2", "b"));
//		IO.println("x1=" + x1);
//		var x2 = f.create(f.actualType(Foo.I.class), Map.of("s2", "c"));
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
