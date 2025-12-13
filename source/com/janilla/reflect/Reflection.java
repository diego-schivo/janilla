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

import java.lang.annotation.Annotation;
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
//		IO.println("Reflection.copy, source=" + source + ", destination=" + destination);
		var c = destination.getClass();
		var s = propertyNames(c);
		if (filter != null)
			s = s.filter(filter);
		var kk = s.toList();
		if (kk == null || kk.isEmpty())
			return destination;
		var vv = kk.stream().map(k -> {
			var v = source.apply(k);
			return v != SKIP_COPY ? Java.mapEntry(k, v) : null;
//		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}).filter(Objects::nonNull).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
		if (vv.isEmpty())
			return destination;
		if (c.isRecord()) {
			var aa = Arrays.stream(c.getRecordComponents()).map(x -> {
//				IO.println("Reflection.copy, x=" + x);
				if (vv.containsKey(x.getName()))
					return vv.get(x.getName());
				var p = property(c, x.getName());
//				return p != null ? p.get(destination) : null;
				if (p != null)
					return p.get(destination);
//				Field f;
//				try {
//					f = c.getDeclaredField(x.getName());
//				} catch (NoSuchFieldException e) {
//					f = null;
//				}
//				if (f != null && f.isAnnotationPresent(Flat.class)) {
//					var c0 = x.getType().getConstructors()[0];
//					Object d;
//					try {
//						d = c0.newInstance(IntStream.range(0, c0.getParameterCount()).mapToObj(_ -> null).toArray());
//					} catch (ReflectiveOperationException e) {
//						throw new RuntimeException(e);
//					}
//					return copy(source, d, filter);
//				}
				try {
					return x.getAccessor().invoke(destination);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}).toArray();
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

	protected static Map<String, Property> propertyMap(Class<?> type) {
//		IO.println("Reflection.compute, class1=" + class1);
		class A {
			private static final Map<Class<?>, Map<String, Property>> RESULTS = new ConcurrentHashMap<>();

			private static Map<String, Property> compute(Class<?> c) {
				if (!Modifier.isPublic(c.getModifiers())) {
					if (Map.Entry.class.isAssignableFrom(c))
						c = Map.Entry.class;
					else if (Supplier.class.isAssignableFrom(c))
						c = Supplier.class;
					else
						throw new IllegalArgumentException();
				}
				var c0 = c;

				var rcc = c.getRecordComponents();
				var mm = new HashMap<String, Member[]>();
				if (rcc != null)
					for (var x : rcc) 
						mm.computeIfAbsent(x.getName(), k -> {
							Method g;
							try {
								g = c0.getMethod(k);
							} catch (NoSuchMethodException e) {
								throw new RuntimeException(e);
							}
							return new Member[] { g, null };
						});

				for (var m : c.getMethods()) {
					if (mm.containsKey(m.getName()) || Modifier.isStatic(m.getModifiers())
							|| m.getDeclaringClass() == Object.class
							|| Set.of("hashCode", "toString").contains(m.getName()))
						continue;

					var g = m.getReturnType() != Void.TYPE && m.getParameterCount() == 0
							&& !(m.getName().startsWith("with") && m.getReturnType() == c) ? m : null;
					var s = m.getReturnType() == Void.TYPE && m.getParameterCount() == 1 ? m : null;

					if (g != null || s != null) {
						var k = Property.name(m);
						var gs = mm.computeIfAbsent(k, _ -> new Member[2]);
						if (g != null)
							gs[0] = g;
						if (s != null)
							gs[1] = s;
					}
				}

				for (var f : c.getFields()) {
					if (!Modifier.isStatic(f.getModifiers()))
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
				return mm.values().stream().map(
						x -> x.length == 1 ? Property.of((Field) x[0]) : Property.of(c0, (Method) x[0], (Method) x[1]))
						.map(x -> {
							Field f;
							try {
								f = c0.getDeclaredField(x.name());
							} catch (NoSuchFieldException e) {
								f = null;
							}
							var o = f != null ? f.getAnnotation(Order.class) : null;
							return new AbstractMap.SimpleImmutableEntry<>(x,
									o != null ? Integer.valueOf(o.value()) : oo != null ? oo.get(x.name()) : null);
						}).sorted(Comparator.comparing(Map.Entry::getValue,
								Comparator.nullsLast(Comparator.naturalOrder())))
						.map(Map.Entry::getKey).flatMap(x -> {
							Field f;
							try {
								f = c0.getDeclaredField(x.name());
							} catch (NoSuchFieldException e) {
								f = null;
							}
							if (f != null && f.isAnnotationPresent(Flat.class)) {
								var m = A.RESULTS.get(f.getType());
								if (m == null)
//									m = propertyMap(f.getType());
									m = compute(f.getType());
								return m.values().stream().filter(y -> !mm.containsKey(y.name()))
										.map(y -> Property.of(x, y));
							}
							return Stream.of(x);
						}).collect(Collectors.toMap(Property::name, p -> p, (_, x) -> x, LinkedHashMap::new));
			}
		}
		return A.RESULTS.computeIfAbsent(type, A::compute);
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

	public static Type[] actualParameterTypes(Method method, Class<?> target) {
//		IO.println("Reflection.actualParameterTypes, method=" + method + ", target=" + target);
		class A {
			private static final Map<Method, Map<Class<?>, Type[]>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(method, _ -> new ConcurrentHashMap<>()).computeIfAbsent(target, _ -> {
			var m = actualTypeArguments(target, method.getDeclaringClass());
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

	private static Object NULL = new Object();

	public static Map<String, Type> actualTypeArguments(Class<?> target, Class<?> superClass) {
//		IO.println("Reflection.actualTypeArguments, target=" + target + ", superClass=" + superClass);
		class A {
			private static final Map<Class<?>, Map<Class<?>, Object>> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(target, _ -> new ConcurrentHashMap<>()).computeIfAbsent(superClass, _ -> {
			Map<String, Type> m = null;
			for (var c = target; c != superClass; c = c.getSuperclass()) {
				var pp = c.getSuperclass().getTypeParameters();
				var aa = c.getGenericSuperclass() instanceof ParameterizedType x ? x.getActualTypeArguments() : null;
				var m2 = new LinkedHashMap<String, Type>();
				for (var i = 0; i < pp.length; i++)
					m2.put(pp[i].getName(), aa[i] instanceof TypeVariable v ? m.get(v.getName()) : aa[i]);
				m = m2;
			}
			return m != null ? m : NULL;
		});
		@SuppressWarnings("unchecked")
		var m = o != NULL ? (Map<String, Type>) o : null;
		return m;
	}

	public static Method bridgeMethod(Method method) {
//		IO.println("Reflection.bridgeMethod, method=" + method);
		if (method.isBridge())
			throw new IllegalArgumentException();
		class A {
			private static final Map<Method, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(method, _ -> {
			var n1 = method.getName();
			var t1 = method.getReturnType();
			var tt1 = method.getParameterTypes();
			m2: for (var m2 : method.getDeclaringClass().getMethods()) {
				var n2 = m2.isBridge() ? m2.getName() : null;
				var t2 = Objects.equals(n2, n1) ? m2.getReturnType() : null;
				var tt2 = t2 != null && t2.isAssignableFrom(t1) ? m2.getParameterTypes() : null;
				if (tt2 != null && tt2.length == tt1.length) {
					for (var i = 0; i < tt2.length; i++)
						if (!tt2[i].isAssignableFrom(tt1[i]))
							continue m2;
					return m2;
				}
			}
			return NULL;
		});
		return o != NULL ? (Method) o : null;
	}

	public static <T extends Annotation> T inheritedAnnotation(Method method, Class<T> annotation) {
//		IO.println("Reflection.inheritedAnnotation, method=" + method + ", annotation=" + annotation);
		class A {
			private static final Map<Method, Map<Class<?>, Object>> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(method, _ -> new ConcurrentHashMap<>()).computeIfAbsent(annotation,
				_ -> Stream.iterate(method, x -> {
					var s = x.getDeclaringClass().getSuperclass();
					if (s != null) {
						var b = x.isBridge() ? x : bridgeMethod(x);
						try {
							return s.getMethod(x.getName(), (b != null ? b : x).getParameterTypes());
						} catch (NoSuchMethodException e) {
						}
					}
					return null;
				}).takeWhile(Objects::nonNull).map(x -> (Object) x.getAnnotation(annotation)).filter(Objects::nonNull)
						.findFirst().orElse(NULL));
		@SuppressWarnings("unchecked")
		var t = o != NULL ? (T) o : null;
		return t;
	}

	public static MethodHandle methodHandle(Method method) {
//		IO.println("Reflection.methodHandle, method=" + method);
		class A {
			private static final Map<Method, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(method, _ -> {
			try {
				return MethodHandles.publicLookup().unreflect(method);
			} catch (ReflectiveOperationException e) {
				return new RuntimeException(e);
			}
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (MethodHandle) o;
	}
}
