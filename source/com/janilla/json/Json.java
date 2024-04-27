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
package com.janilla.json;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.janilla.json.JsonToken.Boundary;
import com.janilla.reflect.Reflection;

public interface Json {

	public static void main(String[] args) {
		var s = """
				{
					"foo": [
						"bar",
						123,
						false,
						null
					],
					"baz": -456.78,
					"qux": true
				}""";
		var o = Json.parse(s);
		System.out.println(o);
		var t = Json.format(o);
		System.out.println(t);
		var p = Json.parse(t);
		System.out.println(p);
		assert p.equals(o) : p;
	}

	static String format(Object object) {
		var t = new JsonIterator();
		t.setObject(object);
		return format(t);
	}

	static String format(Iterator<JsonToken<?>> tokens) {
		var s = Spliterators.spliteratorUnknownSize(tokens, 0);
		return StreamSupport.stream(s, false).collect(formatCollector());
	}

	static Object parse(String string) {
		return parse(string, parseCollector());
	}

	static <T> T parse(String string, Collector<JsonToken<?>, ?, T> collector) {
//		System.out.println("string=" + string);
		var s = new JsonScanner();
		var l = new ArrayList<JsonToken<?>>();
		return IntStream.concat(string.chars(), IntStream.of(-1)).boxed().flatMap(i -> {
			var v = i.intValue();

//			System.out.println(v >= 0 ? Character.toString((char) v) : String.valueOf(v));

			l.clear();
			if (!s.accept(v, l) || (v == -1 && !s.done()))
				throw new RuntimeException();
			return l.stream();
		}).collect(collector);
	}

	static Collector<JsonToken<?>, StringBuilder, String> formatCollector() {
		return Collector.of(() -> new StringBuilder(), new BiConsumer<>() {

			JsonToken<?> previous;

			@Override
			public void accept(StringBuilder a, JsonToken<?> t) {
				switch (t.type()) {
				case OBJECT:
					switch ((Boundary) t.data()) {
					case START:
						a.append('{');
						break;
					case END:
						a.append('}');
						break;
					}
					break;
				case ARRAY:
					switch ((Boundary) t.data()) {
					case START:
						a.append('[');
						break;
					case END:
						a.append(']');
						break;
					}
					break;
				case MEMBER:
					switch ((Boundary) t.data()) {
					case START:
						if (Objects.equals(previous, JsonToken.MEMBER_END))
							a.append(',');
						break;
					default:
						break;
					}
					break;
				case ELEMENT:
					switch ((Boundary) t.data()) {
					case START:
						if (Objects.equals(previous, JsonToken.ELEMENT_END))
							a.append(',');
						break;
					default:
						break;
					}
					break;
				case STRING:
					a.append('"').append((String) t.data()).append('"');
					if (Objects.equals(previous, JsonToken.MEMBER_START))
						a.append(':');
					break;
				case NUMBER, BOOLEAN, NULL:
					a.append(t.data());
					break;
				default:
					break;
				}
				previous = t;
			}
		}, (a, b) -> a, StringBuilder::toString);
	}

	static Collector<JsonToken<?>, ?, Object> parseCollector() {
//		return Collector.of(() -> new ArrayDeque<Object>(), (a, t) -> {
		return Collector.of(() -> new ArrayList<Object>(), (a, t) -> {
//			System.out.println("t=" + t);
			switch (t.type()) {
			case OBJECT:
				switch ((Boundary) t.data()) {
				case START:
					a.add(new HashMap<String, Object>());
					break;
				default:
					break;
				}
				break;
			case ARRAY:
				switch ((Boundary) t.data()) {
				case START:
					a.add(new ArrayList<Object>());
					break;
				default:
					break;
				}
				break;
			case MEMBER:
				switch ((Boundary) t.data()) {
				case END:
					var v = a.remove(a.size() - 1);
					var k = (String) a.remove(a.size() - 1);
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) a.get(a.size() - 1);
					m.put(k, v);
					break;
				default:
					break;
				}
				break;
			case ELEMENT:
				switch ((Boundary) t.data()) {
				case END:
					var e = a.remove(a.size() - 1);
					@SuppressWarnings("unchecked")
					var l = (List<Object>) a.get(a.size() - 1);
					l.add(e);
					break;
				default:
					break;
				}
				break;
			case STRING, NUMBER, BOOLEAN, NULL:
				a.add(t.data());
				break;
			default:
				break;
			}
		}, (a, b) -> a, a -> {
			if (a.size() != 1)
				throw new RuntimeException();
			return a.remove(0);
		});
	}

//	static <T> Collector<JsonToken<?>, ?, T> parseCollector(Class<T> class1) {
//		return Collector.of(() -> (Deque<Object>) /* new ArrayDeque<>() */ new LinkedList<>(), new BiConsumer<>() {
//
//			Deque<Class<?>> b = new ArrayDeque<>();
//
//			JsonToken<?> p;
//
//			{
//				b.push(class1);
//			}
//
//			@Override
//			public void accept(Deque<Object> a, JsonToken<?> t) {
//				switch (t.type()) {
//				case OBJECT:
//					switch ((Boundary) t.data()) {
//					case START:
//						a.push(new HashMap<String, Object>());
//						break;
//					case END:
//						@SuppressWarnings("unchecked")
//						var m = ((Map<String, Object>) a.pop());
//						var c = b.peek();
//						var o = convert2(m, c);
//						a.push(o);
//						break;
//					}
//					break;
//				case ARRAY:
//					switch ((Boundary) t.data()) {
//					case START:
//						a.push(new ArrayList<Object>());
//						break;
//					default:
//						break;
//					}
//					break;
//				case MEMBER:
//					switch ((Boundary) t.data()) {
//					case END:
//						b.pop();
//						var v = a.pop();
//						var k = (String) a.pop();
//						@SuppressWarnings("unchecked")
//						var m = (Map<String, Object>) a.peek();
//						m.put(k, v);
//						break;
//					default:
//						break;
//					}
//					break;
//				case ELEMENT:
//					switch ((Boundary) t.data()) {
//					case END:
//						var e = a.pop();
//						@SuppressWarnings("unchecked")
//						List<Object> l = (List<Object>) a.peek();
//
//						if (b.peek() == Long.class && e instanceof Integer i)
//							e = (long) i;
//
//						l.add(e);
//						break;
//					default:
//						break;
//					}
//					break;
//				case STRING, NUMBER, BOOLEAN, NULL:
//					a.push(t.data());
//					if (Objects.equals(p, JsonToken.MEMBER_START)) {
//						var m = Reflection.property(b.peek(), (String) t.data());
//						Class<?> z = null;
//						if (m.getReturnType().isArray())
//							z = m.getReturnType().componentType();
//						else if (m.getGenericReturnType() instanceof ParameterizedType v) {
//							var x = v.getActualTypeArguments()[0];
//							z = switch (x) {
//							case Class<?> y -> y;
//							case ParameterizedType y -> (Class<?>) y.getRawType();
//							default -> throw new RuntimeException();
//							};
//						}
//						if (z == null)
//							z = m.getReturnType();
//						b.push(z);
//					}
//					break;
//				default:
//					break;
//				}
//				p = t;
//			}
//		}, (a, b) -> a, a -> {
//			if (a.size() != 1)
//				throw new RuntimeException();
//			@SuppressWarnings("unchecked")
//			var t = (T) a.pop();
//			return t;
//		});
//	}

	static Object convert(Object input, Type target, Function<String, Class<?>> typeResolver) {
		var r = getRawType(target);

		if (input == null || (input instanceof String s && s.isEmpty())) {
			if (r == Boolean.TYPE)
				return false;
			if (r == Integer.TYPE)
				return 0;
			if (r == Long.TYPE)
				return 0l;
			if (r == String.class)
				return input;
			return null;
		}

		if (r != Object.class && r.isAssignableFrom(input.getClass()) && !r.isArray()
				&& !Collection.class.isAssignableFrom(r) && !Map.class.isAssignableFrom(r))
			return input;

		if (r == BigDecimal.class)
			return switch (input) {
			case Double x -> BigDecimal.valueOf(x);
			case Long x -> BigDecimal.valueOf(x);
			default -> throw new RuntimeException();
			};
		if (r == Boolean.class || r == Boolean.TYPE)
			return switch (input) {
			case Boolean x -> input;
			case String x -> Boolean.parseBoolean(x);
			default -> throw new RuntimeException();
			};
		if (r == Instant.class)
			return Instant.parse((String) input);
		if (r == Integer.class || r == Integer.TYPE)
			return switch (input) {
			case Integer x -> input;
			case Long x -> x.intValue();
			case String x -> Integer.parseInt(x);
			default -> throw new RuntimeException();
			};
		if (r == LocalDate.class)
			return LocalDate.parse((String) input);
		if (r == Locale.class)
			return Locale.forLanguageTag((String) input);
		if (r == Long.class || r == Long.TYPE)
			return switch (input) {
			case Integer x -> x.longValue();
			case Long x -> input;
			case String x -> Long.parseLong(x);
			default -> throw new RuntimeException();
			};
		if (r == URI.class)
			return URI.create((String) input);
		if (r == UUID.class)
			return UUID.fromString((String) input);
		if (r == byte[].class)
			return Base64.getDecoder().decode((String) input);
		if (r == long[].class)
			return ((Collection<?>) input).stream().mapToLong(x -> (long) x).toArray();

		if (r.isArray() || Collection.class.isAssignableFrom(r)) {
			var s = switch (input) {
			case Object[] x -> Arrays.stream(x);
			case Collection<?> x -> x.stream();
			default -> throw new RuntimeException();
			};
			var t = r.isArray() ? r.componentType()
					: getRawType(((ParameterizedType) target).getActualTypeArguments()[0]);
			s = s.map(y -> convert(y, t, typeResolver));

			if (r.isArray()) {
				if (r.componentType() == Integer.TYPE)
					return s.mapToInt(x -> (Integer) x).toArray();
				else
					return s.toArray(l -> (Object[]) Array.newInstance(t, l));
			}
			if (r == List.class)
				return s.toList();
			if (r == Set.class)
				return s.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		if (input instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			var m = (Map<String, Object>) input;
			if (m.containsKey("$type")) {
				var c = typeResolver.apply((String) m.get("$type"));
				if (!r.isAssignableFrom(c))
					throw new RuntimeException();
				r = c;
			}

			if (r == Map.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				return m.entrySet().stream().map(e -> {
					var k = convert(e.getKey(), aa[0], typeResolver);
					var v = convert(e.getValue(), aa[1], typeResolver);
					return new SimpleEntry<>(k, v);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}
			if (r == Map.Entry.class)
				return new SimpleEntry<>(m.get("key"), m.get("value"));

			// System.out.println("c=" + c);
			var d = r.getConstructors()[0];
			// System.out.println("d=" + d);
			var tt = r.isRecord() ? Arrays.stream(r.getRecordComponents()).collect(
					Collectors.toMap(x -> x.getName(), x -> x.getGenericType(), (x, y) -> x, LinkedHashMap::new))
					: null;
			Object o;
			try {
				if (tt != null) {
					var z = tt.entrySet().stream().map(x -> convert(m.get(x.getKey()), x.getValue(), typeResolver))
							.toArray();
					o = d.newInstance(z);
				} else {
					o = d.newInstance();
					for (var e : m.entrySet()) {
						if (e.getKey().equals("$type"))
							continue;
						// System.out.println("e=" + e);
						var s = Reflection.property(r, e.getKey());
						var v = convert(e.getValue(), s.getGenericType(), typeResolver);
						// System.out.println("s=" + s + ", i=" + i + ", v=" + v);
						s.set(o, v);
					}
				}
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			return o;
		}

		return input;
	}

	static Class<?> getRawType(Type type) {
		return switch (type) {
		case Class<?> x -> x;
		case ParameterizedType x -> (Class<?>) x.getRawType();
		default -> throw new IllegalArgumentException();
		};
	}
}
