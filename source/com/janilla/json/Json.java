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
import java.time.Instant;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.janilla.json.JsonToken.Boundary;
import com.janilla.reflect.Parameter;
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
		var l = new LinkedList<JsonToken<?>>();
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

	static Collector<JsonToken<?>, LinkedList<Object>, Object> parseCollector() {
		return Collector.of(() -> new LinkedList<Object>(), (a, t) -> {
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
					a.push(new LinkedList<Object>());
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
		return Collector.of(() -> (Deque<Object>) new LinkedList<>(), new BiConsumer<>() {

			Deque<Class<?>> b = new LinkedList<>();

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
						var u = d.getParameterAnnotations();
						var n = Arrays.stream(u)
								.map(b -> (Parameter) Arrays.stream(b)
										.filter(e -> e.annotationType() == Parameter.class).findFirst().get())
								.map(Parameter::value).toArray(String[]::new);
						@SuppressWarnings("unchecked")
						var m = ((Map<String, Object>) a.pop());
						Object i;
						try {
							if (n.length > 0 && Arrays.stream(n).allMatch(Objects::nonNull))
								i = d.newInstance(Arrays.stream(n).map(m::get).toArray());
							else {
								i = d.newInstance();
								for (var e : m.entrySet()) {
									var s = Reflection.setter(c, e.getKey());
									var v = s.getParameterTypes()[0];
									var o = e.getValue();
									if (v == Instant.class)
										o = o != null ? Instant.parse((String) o) : null;
									else if (v == Long.class && o instanceof Integer j)
										o = (long) j.intValue();
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
						a.push(new LinkedList<Object>());
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
}
