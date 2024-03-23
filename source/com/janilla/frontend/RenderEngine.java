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
package com.janilla.frontend;

import java.io.IOException;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.io.IO;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.web.Render;

public class RenderEngine {

	protected IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator;

	protected Deque<Entry> stack = new LinkedList<>();

	public void setToInterpolator(
			IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator) {
		this.toInterpolator = toInterpolator;
	}

	public Deque<Entry> getStack() {
		return stack;
	}

	public Object render(Entry input) throws IOException {
		var s = stack.size();
		stack.push(input);
		try {
			for (var x : stack)
				if (x.getValue() instanceof Renderer y && y.evaluate(this))
					break;

			if (input.getValue() == null)
				return null;

			var r = input.getValue().getClass().getAnnotation(Render.class);
			var t = r != null ? r.template() : null;

			r = input.type instanceof AnnotatedType x ? x.getAnnotation(Render.class) : null;
			if (r != null && !r.template().isEmpty())
				t = r.template();

			if (input.getValue() instanceof Object[] oo) {
				var z = switch (input.type) {
				case AnnotatedArrayType u -> u.getAnnotatedGenericComponentType();
				default -> ((AnnotatedParameterizedType) getAnnotatedInterface((AnnotatedType) input.type,
						Iterable.class, Stream.class)).getAnnotatedActualTypeArguments()[0];
				};
				r = z.getAnnotation(Render.class);
				var d = r != null ? r.delimiter() : null;
				var b = new ArrayList<String>();
				for (var i = 0; i < oo.length; i++) {
					var e = oo[i];
					var x = render(new Entry(i, e, z));
					var y = x != null ? x.toString() : null;
					if (y == null || y.isEmpty())
						continue;
					if (!b.isEmpty() && d != null && !d.isEmpty())
						b.add(d);
					b.add(y);
				}
				return b.stream().collect(Collectors.joining());
			}

			if (input.template != null && !input.template.isEmpty())
				t = input.template;
			var i = t != null && !t.isEmpty() ? toInterpolator.apply(t) : null;
			if (i != null)
				return i.apply(x -> {
					try {
						var e = (String) x;
						evaluate(e);
						var c = stack.pop();
						if (e.isEmpty()) {
							c.type = null;
							c.template = null;
						}
						return render(c);
					} finally {
						while (stack.size() > s + 1)
							stack.pop();
					}
				});

			return input.getValue();
		} finally {
			while (stack.size() > s)
				stack.pop();
		}
	}

	public <T> boolean match(Class<T> type, BiConsumer<T, Entry> consumer) {
		var cc = type.getRecordComponents();
		if (stack.size() < cc.length)
			return false;
		var ee = stack.iterator();
		var aa = new Object[cc.length];
		for (var i = cc.length - 1; i >= 0; i--) {
			var c = cc[i];
			var e = ee.next();
			if (e.getKey() instanceof Integer j && c.getType() == Integer.TYPE)
				aa[i] = j;
			else if (e.getValue() == null || c.getName().equals(e.getKey())
					|| (c.getType() != Object.class && c.getType().isAssignableFrom(e.getValue().getClass())))
				aa[i] = e.getValue();
			else
				return false;
		}
		try {
			var c = type.getDeclaredConstructors()[0];
			c.setAccessible(true);
			@SuppressWarnings("unchecked")
			var t = (T) c.newInstance(aa);
			consumer.accept(t, stack.peek());
			return true;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void evaluate(String expression) throws IOException {
		if (expression.isEmpty())
			return;
		var c = stack.peek();
		if (c.getValue() == null)
			return;
		var nn = expression.split("\\.");
		for (var i = 0; i < nn.length; i++) {
			var n = nn[i];

			switch (c.getValue()) {
			case Map<?, ?> m: {
				var v = m.get(n);
				var t = ((AnnotatedParameterizedType) c.type).getAnnotatedActualTypeArguments()[1];
				c = new Entry(n, v, t);
				break;
			}
			case EntryList<?, ?> m: {
				var v = m.get(n);
				var a = c.type;
				var b = EntryList.class;
				var t = getAnnotatedSuperclass(a, b);
				var u = ((AnnotatedParameterizedType) t).getAnnotatedActualTypeArguments()[1];
				c = new Entry(n, v, u);
				break;
			}
			case Function<?, ?> f: {
				@SuppressWarnings("unchecked")
				var g = (Function<String, ?>) f;
				var v = g.apply(n);
				var t = ((AnnotatedParameterizedType) c.type).getAnnotatedActualTypeArguments()[1];
				c = new Entry(n, v, t);
				break;
			}
			default: {
				var g = Reflection.getter(c.getValue().getClass(), n);
				if (g == null && i == 0)
					for (var dd = stack.stream().skip(1).iterator(); dd.hasNext();) {
						var d = dd.next();
						g = Reflection.getter(d.getValue().getClass(), n);
						if (g != null) {
							c = d;
							break;
						}
					}
				Object v;
				AnnotatedType t;
				if (g == null) {
					v = null;
					t = null;
				} else {
					try {
						v = g.invoke(c.getValue());
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					var s = v != null ? switch (v) {
					case Object[] x -> Arrays.stream(x);
					case Iterable<?> x -> StreamSupport.stream(x.spliterator(), false);
					case Stream<?> x -> x;
					default -> null;
					} : null;
					t = g.getAnnotatedReturnType();
					if (s != null) {
						var u = switch (t) {
						case AnnotatedArrayType x -> x.getAnnotatedGenericComponentType();
						default -> ((AnnotatedParameterizedType) getAnnotatedInterface((AnnotatedType) t,
								Iterable.class, Stream.class)).getAnnotatedActualTypeArguments()[0];
						};
						v = s.toArray(l -> (Object[]) Array.newInstance((Class<?>) u.getType(), l));
					}
				}
				c = new Entry(n, v, t);
				break;
			}
			}
			stack.push(c);
			if (c.getValue() == null)
				return;
		}
	}

	protected static AnnotatedType getAnnotatedSuperclass(AnnotatedType type, Class<?> class1) {
		return Stream.iterate(type, a -> getRawType(a).getAnnotatedSuperclass()).filter(a -> getRawType(a) == class1)
				.findAny().get();
	}

	protected static AnnotatedType getAnnotatedInterface(AnnotatedType type, Class<?>... interfaces) {
		return Stream.concat(Stream.of(type), Arrays.stream(getRawType(type).getAnnotatedInterfaces())).filter(a -> {
			var t = getRawType(a);
			return t.isInterface() && Arrays.stream(interfaces).anyMatch(i -> i.isAssignableFrom(t));
		}).findAny().get();
	}

	protected static Class<?> getRawType(AnnotatedType annotated) {
		return (Class<?>) (annotated.getType() instanceof ParameterizedType p ? p.getRawType() : annotated.getType());
	}

	public static class Entry extends SimpleEntry<Object, Object> {

		private static final long serialVersionUID = -7935499563158999871L;

		AnnotatedType type;

		String template;

		public Entry(Object key, Object value, AnnotatedType type) {
			super(key, value);
			this.type = type;
		}

		public AnnotatedType getType() {
			return type;
		}

		public void setType(AnnotatedType type) {
			this.type = type;
		}

		public String getTemplate() {
			return template;
		}

		public void setTemplate(String template) {
			this.template = template;
		}

		@Override
		public String toString() {
			return "[key=" + getKey() + ", value=" + getValue() + ", type=" + type + "]";
		}
	}
}
