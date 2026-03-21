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
package com.janilla.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JavaReflect {

	public static Stream<String> propertyNames(Type type) {
//		IO.println("JavaReflect.properties, type=" + type);
		return propertyMap(type).keySet().stream();
	}

	public static Stream<Property> properties(Type type) {
//		IO.println("JavaReflect.properties, type=" + type);
		return propertyMap(type).values().stream();
	}

	public static Property property(Type type, String name) {
//		IO.println("JavaReflect.property, type=" + type + ", name=" + name);
		return propertyMap(type).get(name);
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
//		IO.println("JavaReflect.copy, source=" + source + ", destination=" + destination);
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
		}).filter(Objects::nonNull).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
		if (vv.isEmpty())
			return destination;
		if (c.isRecord()) {
			var aa = Arrays.stream(c.getRecordComponents()).map(x -> {
//				IO.println("JavaReflect.copy, x=" + x);
				if (vv.containsKey(x.getName()))
					return vv.get(x.getName());
				var p = property(c, x.getName());
				if (p != null)
					return p.get(destination);

				try {
					var f = c.getDeclaredField(x.getName());
					if (f != null && f.isAnnotationPresent(Flat.class)) {
						var aa2 = Arrays.stream(f.getType().getRecordComponents()).map(x2 -> {
							if (vv.containsKey(x2.getName()))
								return vv.get(x2.getName());
							var p2 = property(c, x2.getName());
							return p2 != null ? p2.get(destination) : null;
						}).toArray();
						return f.getType().getConstructors()[0].newInstance(aa2);
					}
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}

				try {
					return x.getAccessor().invoke(destination);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}).toArray();
//			try {
//				@SuppressWarnings("unchecked")
//				var t = (T) constructor(c).newInstance(aa);
//				return t;
//			} catch (ReflectiveOperationException e) {
//				throw new RuntimeException(e);
//			}

			try {
				@SuppressWarnings("unchecked")
				var t = (T) JavaInvoke.methodHandle(constructor(c)).invokeWithArguments(aa);
				return t;
			} catch (Throwable e) {
				switch (e) {
				case RuntimeException x:
					throw x;
				default:
					throw new RuntimeException(e);
				}
			}
		}
		properties(c).filter(x -> vv.containsKey(x.name()) && x.canSet()).forEach(x -> {
//			IO.println("JavaReflect.copy, x=" + x);
			x.set(destination, vv.get(x.name()));
		});
		return destination;
	}

	protected static Map<String, Property> propertyMap(Type type) {
//		IO.println("JavaReflect.propertyMap, type=" + type);

		class A {
			private static final Map<Type, Map<String, Property>> RESULTS = new ConcurrentHashMap<>();

			private static Map<String, Property> compute(Type t) {
				var c = Java.toClass(t);

				if (!Modifier.isPublic(c.getModifiers())) {
					if (Map.Entry.class.isAssignableFrom(c))
						c = Map.Entry.class;
					else if (Supplier.class.isAssignableFrom(c))
						c = Supplier.class;
//					else
//						throw new IllegalArgumentException();
				}

				var c0 = c;

				var rcc = c.getRecordComponents();
				var mm = new LinkedHashMap<String, Member[]>();
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
//					IO.println("JavaReflect.propertyMap, m=" + m);

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
						x -> x.length == 1 ? Property.of((Field) x[0]) : Property.of(t, (Method) x[0], (Method) x[1]))
						.map(x -> {
//							Field f;
//							try {
//								f = c0.getDeclaredField(x.name());
//							} catch (NoSuchFieldException e) {
//								f = null;
//							}
//							var o = f != null ? f.getAnnotation(Order.class) : null;
							var o = x.member() instanceof AnnotatedElement ae ? ae.getAnnotation(Order.class) : null;
							return Java.mapEntry(x,
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
								var ft = actualType(f, t);
								var m = A.RESULTS.get(ft);
								if (m == null)
//									m = propertyMap(f.getType());
									m = compute(ft);
								return m.values().stream().filter(y -> !mm.containsKey(y.name()))
										.map(y -> Property.of(x, y));
							}
							return Stream.of(x);
						}).collect(Collectors.toMap(Property::name, p -> p, (_, x) -> x, LinkedHashMap::new));
			}
		}
		return A.RESULTS.computeIfAbsent(type, A::compute);
	}

//	public static Type resolveTypeVariable(TypeVariable<?> variable, Class<?> class1, Class<?> superclassOrInterface) {
//		IO.println("JavaReflect.resolveTypeVariable, variable=" + variable + ", class1=" + class1
//				+ ", superclassOrInterface=" + superclassOrInterface);
//		var i = 0;
//		for (var x : superclassOrInterface.getTypeParameters()) {
//			if (x == variable)
//				break;
//			i++;
//		}
//		var d = new ArrayDeque<Class<?>>();
//		d.offer(class1);
//		do {
//			var c = d.poll();
//			var t = c.getGenericSuperclass();
//			var ii = c.getGenericInterfaces();
//			var pt = (ParameterizedType) Stream
//					.concat(t != null ? Stream.of(t) : Stream.<Type>empty(), Arrays.stream(ii)).filter(x -> {
//						var c2 = Java.toClass(x);
//						d.offer(c2);
//						return c2 == superclassOrInterface;
//					}).findFirst().orElse(null);
//			if (pt != null)
//				return pt.getActualTypeArguments()[i];
//		} while (!d.isEmpty());
//		return null;
//	}

	public static Type[] actualParameterTypes(Method method, Class<?> target) {
//		IO.println("JavaReflect.actualParameterTypes, method=" + method + ", target=" + target);
		class A {
			private static final Map<Method, Map<Class<?>, Type[]>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(method, _ -> new ConcurrentHashMap<>()).computeIfAbsent(target,
				_ -> Arrays.stream(method.getGenericParameterTypes())
						.map(x -> actualType(x, target, method.getDeclaringClass())).toArray(Type[]::new));
	}

	public static Type actualType(RecordComponent recordComponent, Type target) {
//		IO.println("JavaReflect.actualType, recordComponent=" + recordComponent + ", target=" + target);
//		class A {
//			private static final Map<RecordComponent, Map<Type, Type>> RESULTS = new ConcurrentHashMap<>();
//		}
//		var t = A.RESULTS.computeIfAbsent(recordComponent, _ -> new ConcurrentHashMap<>()).computeIfAbsent(target,
//				_ -> actualType(recordComponent.getGenericType(), target, recordComponent.getDeclaringRecord()));
		var t = actualType(recordComponent.getGenericType(), target, recordComponent.getDeclaringRecord());
//		IO.println("JavaReflect.actualType, t=" + t);
		return t;
	}

	public static Type actualType(Field field, Type target) {
//		IO.println("JavaReflect.actualType, field=" + field + ", target=" + target);
		class A {
			private static final Map<Field, Map<Type, Type>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(field, _ -> new ConcurrentHashMap<>()).computeIfAbsent(target,
				_ -> actualType(field.getGenericType(), target, field.getDeclaringClass()));
	}

	public static Type actualType(Type type, Type target, Class<?> declaring) {
//		IO.println("JavaReflect.actualType, type=" + type + ", target=" + target + ", declaring=" + declaring);
		if (target == declaring || !declaring.isAssignableFrom(Java.toClass(target)))
			return type;

		var m = actualTypeArguments(target, declaring);
		switch (type) {
		case TypeVariable<?> v:
			return m.get(v.getName());
		case ParameterizedType t:
			return new SimpleParameterizedType(t.getRawType(), Arrays.stream(t.getActualTypeArguments())
					.map(y -> y instanceof TypeVariable v ? m.get(v.getName()) : y).toList());
		default:
			return type;
		}
	}

	public static Map<String, Type> actualTypeArguments(Type type, Class<?> superType) {
//		IO.println("JavaReflect.actualTypeArguments, type=" + type + ", superType=" + superType);
		class A {
			private static final Map<Type, Map<Class<?>, Map<String, Type>>> RESULTS = new ConcurrentHashMap<>();
		}
		var r = A.RESULTS.computeIfAbsent(type, _ -> new ConcurrentHashMap<>()).computeIfAbsent(superType, _ -> {
			var c0 = Java.toClass(type);
			var m = Map.<String, Type>of();
			if (type instanceof ParameterizedType x) {
				var pp = c0.getTypeParameters();
				var aa = x.getActualTypeArguments();
				var m2 = new LinkedHashMap<String, Type>();
				for (var i = 0; i < pp.length; i++)
					m2.put(pp[i].getName(),
							aa[i] instanceof TypeVariable v ? (m != null ? m.get(v.getName()) : null) : aa[i]);
				m = m2;
			}

			var ttt = (superType.isInterface() ? inheritedGenericInterfaces(c0)
					: Stream.of(inheritedGenericClasses(c0))).map(x -> x.toList());
			var tt = ttt.filter(x -> x.stream().anyMatch(y -> Java.toClass(y) == superType)).findFirst().orElse(null);
			if (tt != null)
				for (var t : tt) {
					var c = Java.toClass(t);
//					if (c == superType)
//						break;
					var pp = c.getTypeParameters();
					var aa = t instanceof ParameterizedType x ? x.getActualTypeArguments() : null;
					var m2 = new LinkedHashMap<String, Type>();
					for (var i = 0; i < pp.length; i++) {
						var t2 = aa[i] instanceof TypeVariable v ? m.get(v.getName()) : aa[i];
						if (t2 != null)
							m2.put(pp[i].getName(), t2);
					}
					m = m2;
					if (c == superType)
						break;
				}
			return m;
		});
//		IO.println("JavaReflect.actualTypeArguments, r=" + r);
		return r;
	}

	public static Method bridgeMethod(Method method) {
//		IO.println("JavaReflect.bridgeMethod, method=" + method);
		if (method.isBridge())
			throw new IllegalArgumentException();
		class A {
			private static final Map<Method, Optional<Method>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(method, _ -> {
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
					return Optional.of(m2);
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

//	public static <T extends Annotation> T inheritedAnnotation(AnnotatedElement annotated, Class<T> annotation) {
//		switch (annotated) {
//		case Class<?> t:
//			return Stream
//					.concat(Stream.of(t),
//							Stream.concat(inheritedClasses(t).stream(),
//									inheritedInterfaces(t).stream().flatMap(x -> x.stream()).distinct()))
//					.map(x -> x.getAnnotation(annotation)).filter(x -> x != null).findFirst().orElse(null);
//
//		case AnnotatedType at:
//			var a = at.getAnnotation(annotation);
//			if (a != null)
//				return a;
//			var t = Java.toClass(at.getType());
//			return Stream
//					.concat(Stream.of(t),
//							Stream.concat(inheritedClasses(t).stream(),
//									inheritedInterfaces(t).stream().flatMap(x -> x.stream()).distinct()))
//					.map(x -> x.getAnnotation(annotation)).filter(x -> x != null).findFirst().orElse(null);
//
//		case Method m:
//			var c = m.getDeclaringClass();
//			return Stream.concat(Stream.of(inheritedClasses(c)), inheritedInterfaces(c).stream()).flatMap(tt -> {
//				class B {
//					private Method m;
//				}
//				var b = new B();
//				return Stream.concat(Stream.of((Class<?>) null), tt.stream()).map(x -> {
//					if (x == null)
//						return m;
//					var bm = b.m.isBridge() ? b.m : bridgeMethod(b.m);
//					try {
//						return x.getMethod(m.getName(), (bm != null ? bm : b.m).getParameterTypes());
//					} catch (NoSuchMethodException e) {
//						return null;
//					}
//				}).filter(x -> x != null).peek(x -> b.m = x).map(x -> x.getAnnotation(annotation))
//						.filter(x -> x != null);
//			}).findFirst().orElse(null);
//
//		default:
//			throw new IllegalArgumentException("annotated=" + annotated + " (" + annotated.getClass() + ")");
//		}
//	}

	public static Stream<Class<?>> inheritedClasses(Class<?> class1) {
		class A {
			private static final Map<Class<?>, List<Class<?>>> RESULTS = new ConcurrentHashMap<>();
		}
		var cc = A.RESULTS.computeIfAbsent(class1, _ -> Stream.<Class<?>>iterate(class1, x -> x.getSuperclass()).skip(1)
				.takeWhile(x -> x != null).toList());
		return cc.stream();
	}

	public static Stream<Type> inheritedGenericClasses(Class<?> class1) {
		class A {
			private static final Map<Class<?>, List<Type>> RESULTS = new ConcurrentHashMap<>();
		}
		var cc = A.RESULTS.computeIfAbsent(class1,
				_ -> Stream.<Type>iterate(class1, x -> Java.toClass(x).getGenericSuperclass()).skip(1)
						.takeWhile(x -> x != null).toList());
		return cc.stream();
	}

	public static Stream<Stream<Class<?>>> inheritedInterfaces(Class<?> type) {
		class A {
			private static final Map<Class<?>, List<List<Class<?>>>> RESULTS = new ConcurrentHashMap<>();

			@SuppressWarnings({ "rawtypes", "unchecked" })
			private static List<List<Class<?>>> compute(Class<?> t) {
				var ii = t.getInterfaces();
				return ii.length != 0 ? (List) Arrays.stream(ii).flatMap(i -> {
					var ll = compute(i);
					return !ll.isEmpty() ? ll.stream().map(l -> Stream.concat(Stream.of(i), l.stream()).toList())
							: Stream.of(List.of(i));
				}).toList() : List.of();
			}
		}
		var ii = A.RESULTS.computeIfAbsent(type, A::compute);
		return ii.stream().map(x -> x.stream());
	}

	public static Stream<Stream<Type>> inheritedGenericInterfaces(Class<?> type) {
		class A {
			private static final Map<Class<?>, List<List<Type>>> RESULTS = new ConcurrentHashMap<>();

			@SuppressWarnings({ "rawtypes", "unchecked" })
			private static List<List<Type>> compute(Class<?> t) {
				var ii = t.getGenericInterfaces();
				return ii.length != 0 ? (List) Arrays.stream(ii).flatMap(i -> {
					var ll = compute(Java.toClass(i));
					return !ll.isEmpty() ? ll.stream().map(l -> Stream.concat(Stream.of(i), l.stream()).toList())
							: Stream.of(List.of(i));
				}).toList() : List.of();
			}
		}
		var ii = A.RESULTS.computeIfAbsent(type, A::compute);
		return ii.stream().map(x -> x.stream());
	}

	public static Stream<Method> inheritedMethods(Method method) {
		class A {
			private static final Map<Method, List<Method>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(method, _ -> {
			var c = method.getDeclaringClass();
			return Stream.<Supplier<Stream<Stream<Class<?>>>>>of(() -> Stream.of(inheritedClasses(c)),
					() -> inheritedInterfaces(c)).flatMap(x -> x.get()).flatMap(tt -> {
						class B {
							private Method m;
						}
						var b = new B();
						b.m = method;
						return tt.map(x -> {
							var bm = b.m.isBridge() ? b.m : bridgeMethod(b.m);
							try {
								return x.getMethod(method.getName(), (bm != null ? bm : b.m).getParameterTypes());
							} catch (NoSuchMethodException e) {
								return null;
							}
						}).filter(x -> x != null).peek(x -> b.m = x);
					}).toList();
		}).stream();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> AnnotationAndElement<T> inheritedAnnotation(Class<?> type,
			Class<T> annotationClass) {
		class A {
			private static final Map<Class<?>, Map<Class<?>, AnnotationAndElement<?>>> RESULTS = new ConcurrentHashMap<>();
		}
		return (AnnotationAndElement<T>) A.RESULTS.computeIfAbsent(type, _ -> new ConcurrentHashMap<>())
				.computeIfAbsent(annotationClass, _ -> {
					return Stream
							.<Supplier<Stream<Class<?>>>>of(() -> Stream.of(type), () -> inheritedClasses(type),
									() -> inheritedInterfaces(type).flatMap(x -> x).distinct())
							.flatMap(x -> x.get()).map(x -> {
								var a = x.getAnnotation(annotationClass);
								return a != null ? new AnnotationAndElement<>(a, x) : null;
							}).filter(x -> x != null).findFirst().orElse(null);
				});
	}

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T inheritedAnnotation(Method method, Class<T> annotationClass) {
		class A {
			private static final Map<Method, Map<Class<?>, Annotation>> RESULTS = new ConcurrentHashMap<>();
		}
		return (T) A.RESULTS.computeIfAbsent(method, _ -> new ConcurrentHashMap<>()).computeIfAbsent(annotationClass,
				_ -> {
					return Stream.<Supplier<Stream<Method>>>of(() -> Stream.of(method), () -> inheritedMethods(method))
							.flatMap(x -> x.get()).map(x -> x.getAnnotation(annotationClass)).filter(x -> x != null)
							.findFirst().orElse(null);
				});
	}

	public static Constructor<?> constructor(Class<?> class1) {
		class A {
			private static final Map<Class<?>, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(class1, _ -> {
//			IO.println("JavaReflect.constructor, class1=" + class1);
			var cc = class1.getConstructors();
			cc = cc.length != 0 ? cc : class1.getDeclaredConstructors();
			if (cc.length == 0)
				throw new IllegalArgumentException("class1=" + class1);
			return cc[0];
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (Constructor<?>) o;
	}
}
