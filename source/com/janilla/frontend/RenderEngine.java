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

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.web.Render;

public class RenderEngine {

	protected Function<String, Function<Function<Object, Object>, String>> toInterpolator;

	protected Deque<Entry> stack = new ArrayDeque<>();

	public void setToInterpolator(Function<String, Function<Function<Object, Object>, String>> toInterpolator) {
		this.toInterpolator = toInterpolator;
	}

	public Deque<Entry> getStack() {
		return stack;
	}

	public Object render(Entry input) {
		stack.push(input);
		return render();
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
//			System.out.println("e=" + e);
			if (e.getKey() instanceof Integer j && c.getType() == Integer.TYPE)
				aa[i] = j;
			else if (c.getName().equals(e.getKey()) || (c.getType() != Object.class && e.getValue() != null
					&& c.getType().isAssignableFrom(e.getValue().getClass())))
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

	protected Object render() {
		var i = stack.peek();
		var s = stack.size();
		try {
			if (i.getValue() == null)
				return null;

			var r = !i.ignore ? i.getValue().getClass().getAnnotation(Render.class) : null;
			var t = r != null ? (!r.template().isEmpty() ? r.template() : r.value()) : null;

			r = !i.ignore && i.type instanceof AnnotatedType x ? x.getAnnotation(Render.class) : null;
			if (i.ignore)
				;
			else if (i.template != null && !i.template.isEmpty())
				t = i.template;
			else if (r != null) {
				var u = !r.template().isEmpty() ? r.template() : r.value();
				if (!u.isEmpty())
					t = u;
			}

			if ((t == null || t.isEmpty()) && i.getValue() instanceof Object[] oo) {
				var z = switch (i.type) {
				case AnnotatedArrayType x -> x.getAnnotatedGenericComponentType();
				default -> ((AnnotatedParameterizedType) getAnnotatedInterface((AnnotatedType) i.type, Iterable.class,
						Stream.class)).getAnnotatedActualTypeArguments()[0];
				};
				r = z.getAnnotation(Render.class);
				var d = r != null ? r.delimiter() : null;
				var b = new ArrayList<String>();
				for (var j = 0; j < oo.length; j++) {
					var e = oo[j];
					stack.push(entryOf(j, e, z));
					for (var x : stack)
						if (x.getValue() instanceof RenderParticipant y && y.render(this))
							break;
					var x = render();
					var y = x != null ? x.toString() : null;
					if (y == null || y.isEmpty())
						continue;
					if (!b.isEmpty() && d != null && !d.isEmpty())
						b.add(d);
					b.add(y);
				}
				return b.stream().collect(Collectors.joining());
			}

			var j = t != null && !t.isEmpty() ? toInterpolator.apply(t) : null;
			if (j != null)
				return j.apply(x -> {
					var e = (String) x;
					Entry c = null;
					try {
						evaluate(e);
						c = stack.peek();
						if (e.isEmpty()) {
							c.ignore = true;
						}
						return render();
					} finally {
						if (e.isEmpty()) {
							if (c != null) {
								c.ignore = false;
								stack.push(c);
							}
						} else
							while (stack.size() > s)
								stack.pop();
					}
				});

			var v = i.getValue();
			return v instanceof Locale x ? x.toLanguageTag() : v;
		} finally {
			while (stack.size() >= s)
				stack.pop();
		}
	}

	protected void evaluate(String expression) {
		if (expression.isEmpty())
			return;
		var c = stack.peek();
		if (c.getValue() == null)
			return;
		var nn = expression.split("\\.");
		for (var i = 0; i < nn.length; i++) {
			var n = nn[i];

			int k;
			if (n.endsWith("]")) {
				var j = n.lastIndexOf('[');
				k = Integer.parseInt(n.substring(j + 1, n.length() - 1));
				n = n.substring(0, j);
			} else
				k = -1;

			switch (c.getValue()) {
			case Map<?, ?> m: {
				var v = m.get(n);
				var t = ((AnnotatedParameterizedType) c.type).getAnnotatedActualTypeArguments()[1];
				c = entryOf(n, v, t);
				break;
			}
			case EntryList<?, ?> m: {
				var v = m.get(n);
				var a = c.type;
				var b = EntryList.class;
				var t = getAnnotatedSuperclass(a, b);
				var u = ((AnnotatedParameterizedType) t).getAnnotatedActualTypeArguments()[1];
				c = entryOf(n, v, u);
				break;
			}
			case Function<?, ?> f: {
				@SuppressWarnings("unchecked")
				var g = (Function<String, ?>) f;
				var v = g.apply(n);
				var t = ((AnnotatedParameterizedType) c.type).getAnnotatedActualTypeArguments()[1];
				c = entryOf(n, v, t);
				break;
			}
			default: {
				var g = Reflection.property(c.getValue().getClass(), n);
				if (g == null && i == 0)
					for (var dd = stack.stream().skip(1).iterator(); dd.hasNext();) {
						var d = dd.next();
						g = Reflection.property(d.getValue().getClass(), n);
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
					v = g.get(c.getValue());
					t = g.getAnnotatedType();
				}
				c = entryOf(n, v, t);
				break;
			}
			}
			stack.push(c);

			for (var x : stack)
				if (x.getValue() instanceof RenderParticipant y && y.render(this))
					break;

			if (k >= 0 && c.getValue() instanceof Object[] oo) {
				c = entryOf(k, oo != null && k < oo.length ? oo[k] : null,
						((AnnotatedParameterizedType) c.type).getAnnotatedActualTypeArguments()[0]);
				stack.push(c);
			}
			if (c.getValue() == null)
				break;
		}
	}

	protected Entry entryOf(Object key, Object value, AnnotatedType type) {
		return Entry.of(key, value, type);
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
		var t = annotated.getType() instanceof ParameterizedType p ? p.getRawType() : annotated.getType();
		return t instanceof Class<?> c ? c : Object.class;
	}

	public static class Entry extends AbstractMap.SimpleEntry<Object, Object> {

		private static final long serialVersionUID = -7935499563158999871L;

		public static Entry of(Object key, Object value, AnnotatedType type) {
			var s = value != null ? switch (value) {
			case Object[] x -> Arrays.stream(x);
			case Iterable<?> x -> StreamSupport.stream(x.spliterator(), false);
			case Stream<?> x -> x;
			default -> null;
			} : null;
			if (s != null) {
				var u = switch (type) {
				case AnnotatedArrayType x -> x.getAnnotatedGenericComponentType();
				default -> ((AnnotatedParameterizedType) getAnnotatedInterface((AnnotatedType) type, Iterable.class,
						Stream.class)).getAnnotatedActualTypeArguments()[0];
				};
				value = s.toArray(l -> (Object[]) Array.newInstance(getRawType(u), l));
			}
			return new Entry(key, value, type);
		}

		AnnotatedType type;

		String template;

		boolean ignore;

		private Entry(Object key, Object value, AnnotatedType type) {
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
			return "[key=" + getKey() + ", value=" + getValue() + ", type=" + type + ", template=" + template
					+ ", ignore=" + ignore + "]";
		}
	}
}
