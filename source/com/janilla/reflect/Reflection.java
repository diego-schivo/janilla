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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.java.Java;

public class Reflection {

	public static Stream<String> propertyNames(Class<?> class1) {
//		IO.println("Reflection.properties, class1=" + class1);
		return propertyMap(class1).keySet().stream();
	}

	public static Stream<Property> properties(Class<?> class1) {
//		IO.println("Reflection.properties, class1=" + class1);
		return propertyMap(class1).values().stream();
	}

	public static Property property(Class<?> class1, String name) {
//		IO.println("Reflection.property, class1=" + class1 + ", name=" + name);
		return propertyMap(class1).get(name);
	}

	public static <T> T copy(Object source, T destination) {
		return copy(source, destination, null);
	}

	protected static final Object SKIP_COPY = new Object();

	public static <T> T copy(Object source, T destination, Predicate<String> filter) {
		if (source instanceof Map<?, ?> m)
			return copy(x -> m.containsKey(x) ? m.get(x) : SKIP_COPY, destination, filter);
		var c = source.getClass();
		return copy(x -> {
			var p = property(c, x);
			return p != null ? p.get(source) : SKIP_COPY;
		}, destination, filter);
	}

	protected static <T> T copy(Function<String, Object> source, T destination, Predicate<String> filter) {
		var c = destination.getClass();
		var s = propertyNames(c);
		if (filter != null)
			s = s.filter(filter);
		var kk = s.toList();
		if (kk == null || kk.isEmpty())
			return destination;
		var vv = kk.stream().map(k -> {
			var v = source.apply(k);
			return v != SKIP_COPY ? new AbstractMap.SimpleImmutableEntry<>(k, v) : null;
		}).filter(Objects::nonNull).collect(HashMap::new, (m, kv) -> m.put(kv.getKey(), kv.getValue()), Map::putAll);
		if (vv.isEmpty())
			return destination;
		if (c.isRecord()) {
			var aa = Arrays.stream(c.getRecordComponents()).map(
					x -> vv.containsKey(x.getName()) ? vv.get(x.getName()) : property(c, x.getName()).get(destination))
					.toArray();
			try {
				@SuppressWarnings("unchecked")
				var t = (T) c.getConstructors()[0].newInstance(aa);
				return t;
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
		properties(c).filter(x -> vv.containsKey(x.name()) && x.canSet()).forEach(x -> {
//			IO.println("Reflection.copy, x=" + x);
			x.set(destination, vv.get(x.name()));
		});
		return destination;
	}

	private static Map<String, Property> propertyMap(Class<?> type) {
//		IO.println("Reflection.compute, class1=" + class1);
		class A {
			private static final Map<Class<?>, Map<String, Property>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(type, class1 -> {
			if (!Modifier.isPublic(class1.getModifiers())) {
				if (Map.Entry.class.isAssignableFrom(class1))
					class1 = Map.Entry.class;
				else if (Supplier.class.isAssignableFrom(class1))
					class1 = Supplier.class;
				else
					throw new IllegalArgumentException();
			}
			var c = class1;
			var rcc = c.getRecordComponents();
			var mm = new HashMap<String, Member[]>();
			if (rcc != null)
				for (var x : rcc) {
					mm.computeIfAbsent(x.getName(), k -> {
						Method g;
						try {
							g = c.getMethod(k);
						} catch (NoSuchMethodException e) {
							throw new RuntimeException(e);
						}
						return new Member[] { g, null };
					});
				}
			for (var m : c.getMethods()) {
				if (mm.containsKey(m.getName()) || Modifier.isStatic(m.getModifiers())
						|| m.getDeclaringClass() == Object.class
						|| Set.of("hashCode", "toString").contains(m.getName()))
					continue;
				var g = m.getReturnType() != Void.TYPE && m.getParameterCount() == 0 ? m : null;
				var s = m.getReturnType() == Void.TYPE && m.getParameterCount() == 1 ? m : null;
				if (g != null && g.getName().startsWith("with") && g.getReturnType() == c)
					g = null;
				if (g != null || s != null) {
					var k = Property.name(m);
					var b = mm.computeIfAbsent(k, _ -> new Member[2]);
					if (g != null)
						b[0] = g;
					if (s != null)
						b[1] = s;
				}
			}
			for (var f : c.getFields()) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				mm.computeIfAbsent(Property.name(f), _ -> new Member[] { f });
			}
			if (c.isArray())
				mm.computeIfAbsent("length", _ -> {
					Method m;
					try {
						m = Array.class.getMethod("getLength", Object.class);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
					return new Member[] { m, null };
				});
			var oo = rcc != null ? IntStream.range(0, rcc.length).boxed()
					.collect(Collectors.toMap(i -> rcc[i].getName(), i -> i + 1)) : null;
			return mm.values().stream()
					.map(x -> x.length == 1 ? Property.of((Field) x[0]) : Property.of(c, (Method) x[0], (Method) x[1]))
					.map(x -> {
						Field f;
						try {
							f = c.getDeclaredField(x.name());
						} catch (NoSuchFieldException e) {
							f = null;
						}
						var o = f != null ? f.getAnnotation(Order.class) : null;
						return new AbstractMap.SimpleImmutableEntry<>(x,
								o != null ? Integer.valueOf(o.value()) : oo != null ? oo.get(x.name()) : null);
					})
					.sorted(Comparator.comparing(Map.Entry::getValue, Comparator.nullsLast(Comparator.naturalOrder())))
					.map(Map.Entry::getKey).flatMap(x -> {
						Field f;
						try {
							f = c.getDeclaredField(x.name());
						} catch (NoSuchFieldException e) {
							f = null;
						}
						if (f != null && f.isAnnotationPresent(Flatten.class)) {
							var m = A.RESULTS.get(f.getType());
							if (m == null)
								m = propertyMap(f.getType());
							return m.values().stream().filter(y -> !mm.containsKey(y.name()))
									.map(y -> Property.of(x, y));
						}
						return Stream.of(x);
					}).collect(Collectors.toMap(Property::name, p -> p, (_, x) -> x, LinkedHashMap::new));
		});
	}

	public static Type resolveTypeVariable(TypeVariable<?> variable, Class<?> class1, Class<?> superclassOrInterface) {
//		IO.println("Reflection.resolveTypeVariable, variable=" + variable + ", class1=" + class1
//				+ ", superclassOrInterface=" + superclassOrInterface);
		var i = 0;
		for (var x : superclassOrInterface.getTypeParameters()) {
			if (x == variable)
				break;
			i++;
		}
		var pt = (ParameterizedType) Stream
				.concat(Stream.of(class1.getGenericSuperclass()), Arrays.stream(class1.getGenericInterfaces()))
				.filter(x -> x != null && Java.toClass(x) == superclassOrInterface).findFirst().get();
		return pt.getActualTypeArguments()[i];
	}

	public static Type[] getActualParameterTypes(Method method, Class<?> target) {
		class A {
			private static final Map<Method, Map<Class<?>, Type[]>> RESULTS = new ConcurrentHashMap<>();
		}
		var m1 = A.RESULTS.computeIfAbsent(method, _ -> new ConcurrentHashMap<>());
		return m1.computeIfAbsent(target, _ -> {
			var m = getActualTypeArguments(target, method.getDeclaringClass());
			return Arrays.stream(method.getGenericParameterTypes()).map(x -> {
				switch (x) {
				case TypeVariable<?> v:
					return m.get(v.getName());
				case ParameterizedType t:
					return new SimpleParameterizedType(t.getRawType(), Arrays.stream(t.getActualTypeArguments())
							.map(y -> y instanceof TypeVariable v ? m.get(v.getName()) : y).toArray(Type[]::new), null);
				default:
					return x;
				}
			}).toArray(Type[]::new);
		});
	}

	public static Map<String, Type> getActualTypeArguments(Class<?> target, Class<?> superClass) {
		class A {
			private static final Map<Class<?>, Map<Class<?>, Map<String, Type>>> RESULTS = new ConcurrentHashMap<>();
		}
		var m1 = A.RESULTS.computeIfAbsent(target, _ -> new ConcurrentHashMap<>());
		return m1.computeIfAbsent(superClass, _ -> {
			Map<String, Type> m = null;
			for (var c = target; c != superClass; c = c.getSuperclass()) {
				var pp = c.getSuperclass().getTypeParameters();
				var aa = ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments();
				var m0 = m;
				m = IntStream.range(0, pp.length).<Map<String, Type>>collect(LinkedHashMap::new,
						(x, i) -> x.put(pp[i].getName(), aa[i] instanceof TypeVariable v ? m0.get(v.getName()) : aa[i]),
						Map::putAll);
			}
			return m;
		});
	}

	public static MethodHandle methodHandle(Method method) {
		class A {
			private static final Map<Method, MethodHandle> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(method, x -> {
			try {
				return MethodHandles.publicLookup().unreflect(x);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
