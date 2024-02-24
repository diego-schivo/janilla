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
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.io.IO;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.web.Render;

public class RenderEngine {

	public static void main(String[] args) throws IOException {
	}

	protected IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator;

	protected LinkedList<ObjectAndType> stack = new LinkedList<>();

	public void setToInterpolator(
			IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator) {
		this.toInterpolator = toInterpolator;
	}

	public List<ObjectAndType> getStack() {
		return stack;
	}

	public Object getObject() {
		return stack.peek().object;
	}

	public Object render(ObjectAndType input) throws IOException {
		if (input.object == null)
			return null;
		stack.push(input);
		try {
			var r = input.type != null ? input.type.getAnnotation(Render.class) : null;
			if (r == null)
				r = input.object.getClass().getAnnotation(Render.class);
			var i = switch (input.object) {
			case Iterable<?> x -> x.iterator();
			case Stream<?> x -> x.iterator();
			default -> null;
			};
			if (i != null) {
//				System.out.println("input.type=" + input.type);
				var t = ((AnnotatedParameterizedType) getAnnotatedInterface(input.type, Iterable.class, Stream.class))
						.getAnnotatedActualTypeArguments()[0];
				var b = Stream.<String>builder();
				var d = r != null ? r.delimiter() : null;
				while (i.hasNext()) {
					var x = render(new ObjectAndType(i.next(), t));
					b.add(x.toString());
					if (i.hasNext() && r != null && !d.isEmpty())
						b.add(d);
				}
				input = new ObjectAndType(b.build().collect(Collectors.joining()), input.type);
			}

			var t = r != null ? r.template() : null;
			var j = t != null && !t.isEmpty() ? toInterpolator.apply(t) : null;
			var c = input;
			if (j != null)
				return j.apply(x -> evaluate((String) x, c));
			for (var x : stack)
				if (x.object instanceof Renderer y) {
					var z = y.render(this);
					if (z != Renderer.CANNOT_RENDER)
						return z;
				}
			return input.object;
		} finally {
			stack.pop();
		}
	}

	Object evaluate(String expression, ObjectAndType context) throws IOException {
		var o = context.object;
		if (o == null)
			return null;
		if (expression.isEmpty())
			return o;
		var i = expression.indexOf('.');
		var n = i >= 0 ? expression.substring(0, i) : expression;
		ObjectAndType p;
		switch (o) {
		case Map<?, ?> m: {
			var v = m.get(n);
			var t = ((AnnotatedParameterizedType) context.type).getAnnotatedActualTypeArguments()[1];
			p = new ObjectAndType(v, t);
			break;
		}
		case EntryList<?, ?> m: {
			var v = m.get(n);
			var a = context.type;
			var b = EntryList.class;
			var t = getAnnotatedSuperclass(a, b);
			var u = ((AnnotatedParameterizedType) t).getAnnotatedActualTypeArguments()[1];
			p = new ObjectAndType(v, u);
			break;
		}
		case Function<?, ?> f: {
			@SuppressWarnings("unchecked")
			var g = (Function<String, ?>) f;
			var v = g.apply(n);
			var t = ((AnnotatedParameterizedType) context.type).getAnnotatedActualTypeArguments()[1];
			p = new ObjectAndType(v, t);
			break;
		}
		default: {
			var g = Reflection.getter(o.getClass(), n);
			if (g == null)
				throw new NullPointerException(o + " " + n);
			Object v;
			try {
				v = g.invoke(o);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			var t = g.getAnnotatedReturnType();
			p = new ObjectAndType(v, t);
			break;
		}
		}
		if (i >= 0) {
			var v = evaluate(expression.substring(i + 1), p);
			p = new ObjectAndType(v, p.type);
		}
		return render(p);
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

	public record ObjectAndType(Object object, AnnotatedType type) {
	}
}
