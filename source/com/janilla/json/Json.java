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

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.BiConsumer;
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
		return format(new JsonIterator(object));
	}

	static String format(Iterator<JsonToken<?>> tokens) {
		var s = Spliterators.spliteratorUnknownSize(tokens, 0);
		return StreamSupport.stream(s, false).collect(formatCollector());
	}

	static Object parse(String string) {
		return parse(string, parseCollector());
	}

	static <T> T parse(String string, Collector<JsonToken<?>, ?, T> collector) {
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

	static Collector<JsonToken<?>, Deque<Object>, Object> parseCollector() {
		return Collector.of(() -> new ArrayDeque<Object>(), (a, t) -> {
			switch (t.type()) {
			case OBJECT:
				switch ((Boundary) t.data()) {
				case START:
					a.push(new HashMap<String, Object>());
					break;
				default:
					break;
				}
				break;
			case ARRAY:
				switch ((Boundary) t.data()) {
				case START:
					a.push(new ArrayList<Object>());
					break;
				default:
					break;
				}
				break;
			case MEMBER:
				switch ((Boundary) t.data()) {
				case END:
					var v = a.pop();
					var k = (String) a.pop();
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) a.peek();
					m.put(k, v);
					break;
				default:
					break;
				}
				break;
			case ELEMENT:
				switch ((Boundary) t.data()) {
				case END:
					var e = a.pop();
					@SuppressWarnings("unchecked")
					var l = (List<Object>) a.peek();
					l.add(e);
					break;
				default:
					break;
				}
				break;
			case STRING, NUMBER, BOOLEAN, NULL:
				a.push(t.data());
				break;
			default:
				break;
			}
		}, (a, b) -> a, a -> {
			if (a.size() != 1)
				throw new RuntimeException();
			return a.pop();
		});
	}

	static <T> Collector<JsonToken<?>, ?, T> parseCollector(Class<T> class1) {
		return Collector.of(() -> (Deque<Object>) /* new ArrayDeque<>() */ new LinkedList<>(), new BiConsumer<>() {

			Deque<Class<?>> b = new ArrayDeque<>();

			JsonToken<?> p;

			{
				b.push(class1);
			}

			@Override
			public void accept(Deque<Object> a, JsonToken<?> t) {
				switch (t.type()) {
				case OBJECT:
					switch ((Boundary) t.data()) {
					case START:
						a.push(new HashMap<String, Object>());
						break;
					case END:
						var c = b.peek();
//						System.out.println("c=" + c);
						var d = c.getConstructors()[0];
//						System.out.println("d=" + d);
						var n = c.isRecord() ? Arrays.stream(c.getRecordComponents()).collect(
								Collectors.toMap(x -> x.getName(), x -> x.getType(), (x, y) -> x, LinkedHashMap::new))
								: null;
						@SuppressWarnings("unchecked")
						var m = ((Map<String, Object>) a.pop());
						Object i;
						try {
							if (n != null) {
								var z = n.entrySet().stream().map(x -> convert(m.get(x.getKey()), x.getValue()))
										.toArray();
								i = d.newInstance(z);
							} else {
								i = d.newInstance();
								for (var e : m.entrySet()) {
//									System.out.println("e=" + e);
									var s = Reflection.setter(c, e.getKey());
									var o = convert(e.getValue(), s.getParameterTypes()[0]);
//									System.out.println("s=" + s + ", i=" + i + ", o=" + o);
									s.invoke(i, o);
								}
							}
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
						a.push(i);
						break;
					}
					break;
				case ARRAY:
					switch ((Boundary) t.data()) {
					case START:
						a.push(new ArrayList<Object>());
						break;
					default:
						break;
					}
					break;
				case MEMBER:
					switch ((Boundary) t.data()) {
					case END:
						b.pop();
						var v = a.pop();
						var k = (String) a.pop();
						@SuppressWarnings("unchecked")
						var m = (Map<String, Object>) a.peek();
						m.put(k, v);
						break;
					default:
						break;
					}
					break;
				case ELEMENT:
					switch ((Boundary) t.data()) {
					case END:
						var e = a.pop();
						@SuppressWarnings("unchecked")
						List<Object> l = (List<Object>) a.peek();

						if (b.peek() == Long.class && e instanceof Integer i)
							e = (long) i;

						l.add(e);
						break;
					default:
						break;
					}
					break;
				case STRING, NUMBER, BOOLEAN, NULL:
					a.push(t.data());
					if (Objects.equals(p, JsonToken.MEMBER_START)) {
						var m = Reflection.getter(b.peek(), (String) t.data());
						var u = m.getGenericReturnType();
						var w = u instanceof ParameterizedType v ? v.getActualTypeArguments()[0] : u;
						var y = w instanceof Class<?> x ? x : null;
						b.push(y);
					}
					break;
				default:
					break;
				}
				p = t;
			}
		}, (a, b) -> a, a -> {
			if (a.size() != 1)
				throw new RuntimeException();
			@SuppressWarnings("unchecked")
			var t = (T) a.pop();
			return t;
		});
	}

	static Object convert(Object input, Class<?> target) {
		if (input == null || (input instanceof String s && s.isEmpty())) {
			if (target == Boolean.TYPE)
				return false;
			if (target == Integer.TYPE)
				return 0;
			if (target == Long.TYPE)
				return 0l;
			if (target == String.class)
				return input;
			return null;
		}

		if (target.isAssignableFrom(input.getClass()))
			return input;
		if (target == BigDecimal.class)
			return switch (input) {
			case Double x -> BigDecimal.valueOf(x);
			case Long x -> BigDecimal.valueOf(x);
			default -> throw new RuntimeException();
			};
		if (target == Boolean.class || target == Boolean.TYPE)
			return switch (input) {
			case Boolean x -> input;
			case String x -> Boolean.parseBoolean(x);
			default -> throw new RuntimeException();
			};
		if (target == Instant.class)
			return Instant.parse((String) input);
		if (target == Integer.class || target == Integer.TYPE)
			return switch (input) {
			case Integer x -> input;
			case Long x -> x.intValue();
			case String x -> Integer.parseInt(x);
			default -> throw new RuntimeException();
			};
		if (target == LocalDate.class)
			return LocalDate.parse((String) input);
		if (target == Long.class || target == Long.TYPE)
			return switch (input) {
			case Integer x -> x.longValue();
			case Long x -> input;
			case String x -> Long.parseLong(x);
			default -> throw new RuntimeException();
			};
		if (target == URI.class)
			return URI.create((String) input);
		if (target == UUID.class)
			return UUID.fromString((String) input);

		if (target == Set.class)
			return switch (input) {
			case Set<?> x -> input;
			case List<?> x -> new LinkedHashSet<>(x);
			default -> throw new RuntimeException();
			};

		return input;
	}
}
