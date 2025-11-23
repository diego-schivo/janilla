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
		IO.println(o);
		var t = Json.format(o);
		IO.println(t);
		var p = Json.parse(t);
		IO.println(p);
		assert p.equals(o) : p;
	}

	static String format(Object object) {
		return format(object, false);
	}

	static String format(Object object, boolean reflection) {
		return format(reflection ? new ReflectionJsonIterator(object, false) : new JsonIterator(object));
	}

	static String format(Iterator<JsonToken<?>> tokens) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(tokens, 0), false).collect(formatCollector());
	}

	static Object parse(String string) {
		return parse(string, parseCollector());
	}

	static <T> T parse(String string, Collector<JsonToken<?>, ?, T> collector) {
//		IO.println("Json.parse, string=" + string);
		var s = new JsonScanner();
		var tt = new ArrayList<JsonToken<?>>();
		return IntStream.concat(string.chars(), IntStream.of(-1)).boxed().flatMap(x -> {
			var v = x.intValue();
//			IO.out.println(v != -1 ? Character.toString((char) v) : String.valueOf(v));
			tt.clear();
			if (!s.accept(v, tt) || (v == -1 && !s.done()))
				throw new RuntimeException();
			return tt.stream();
		}).collect(collector);
	}

	static Collector<JsonToken<?>, StringBuilder, String> formatCollector() {
		return Collector.of(() -> new StringBuilder(), new BiConsumer<>() {

			private JsonToken<?> previous;

			@Override
			public void accept(StringBuilder b, JsonToken<?> t) {
				switch (t.type()) {
				case OBJECT:
					b.append(switch ((JsonToken.Boundary) t.data()) {
					case START -> '{';
					case END -> '}';
					});
					break;
				case ARRAY:
					b.append(switch ((JsonToken.Boundary) t.data()) {
					case START -> '[';
					case END -> ']';
					});
					break;
				case MEMBER:
					switch ((JsonToken.Boundary) t.data()) {
					case START:
						if (Objects.equals(previous, JsonToken.MEMBER_END))
							b.append(',');
						break;
					case END:
						break;
					}
					break;
				case ELEMENT:
					switch ((JsonToken.Boundary) t.data()) {
					case START:
						if (Objects.equals(previous, JsonToken.ELEMENT_END))
							b.append(',');
						break;
					case END:
						break;
					}
					break;
				case STRING:
					b.append('"').append((String) t.data()).append('"');
					if (Objects.equals(previous, JsonToken.MEMBER_START))
						b.append(':');
					break;
				case NUMBER, BOOLEAN, NULL:
					b.append(t.data());
					break;
				default:
					break;
				}
				previous = t;
			}
		}, (_, x) -> x, StringBuilder::toString);
	}

	static Collector<JsonToken<?>, ?, Object> parseCollector() {
		return Collector.of(() -> new ArrayList<>(), (oo, t) -> {
//			IO.println("t=" + t);
			switch (t.type()) {
			case OBJECT:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
					oo.add(new LinkedHashMap<String, Object>());
					break;
				case END:
					break;
				}
				break;
			case ARRAY:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
					oo.add(new ArrayList<Object>());
					break;
				case END:
					break;
				}
				break;
			case MEMBER:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
					break;
				case END:
					var v = oo.removeLast();
					var k = (String) oo.removeLast();
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) oo.getLast();
					m.put(k, v);
					break;
				}
				break;
			case ELEMENT:
				switch ((JsonToken.Boundary) t.data()) {
				case START:
					break;
				case END:
					var x = oo.removeLast();
					@SuppressWarnings("unchecked")
					var l = (List<Object>) oo.getLast();
					l.add(x);
					break;
				}
				break;
			case STRING, NUMBER, BOOLEAN, NULL:
				oo.add(t.data());
				break;
			default:
				break;
			}
		}, (_, x) -> x, x -> {
			if (x.size() != 1)
				throw new RuntimeException();
			return x.remove(0);
		});
	}
}
