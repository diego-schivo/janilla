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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

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
		return format(object, false);
	}

	static String format(Object object, boolean reflection) {
		var t = reflection ? new ReflectionJsonIterator() : new JsonIterator();
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
					switch ((JsonToken.Boundary) t.data()) {
					case START:
						a.append('{');
						break;
					case END:
						a.append('}');
						break;
					}
					break;
				case ARRAY:
					switch ((JsonToken.Boundary) t.data()) {
					case START:
						a.append('[');
						break;
					case END:
						a.append(']');
						break;
					}
					break;
				case MEMBER:
					switch ((JsonToken.Boundary) t.data()) {
					case START:
						if (Objects.equals(previous, JsonToken.MEMBER_END))
							a.append(',');
						break;
					default:
						break;
					}
					break;
				case ELEMENT:
					switch ((JsonToken.Boundary) t.data()) {
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
		return Collector.of(() -> new ArrayList<Object>(), (a, t) -> {
//			System.out.println("t=" + t);
			switch (t.type()) {
			case OBJECT:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
//					a.add(new HashMap<String, Object>());
					a.add(new LinkedHashMap<String, Object>());
					break;
				default:
					break;
				}
				break;
			case ARRAY:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
					a.add(new ArrayList<Object>());
					break;
				default:
					break;
				}
				break;
			case MEMBER:
				switch ((JsonToken.Boundary) t.data()) {
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
				switch ((JsonToken.Boundary) t.data()) {
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
}
