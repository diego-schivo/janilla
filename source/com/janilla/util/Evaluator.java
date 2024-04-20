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
//package com.janilla.util;
//
//import java.lang.reflect.Method;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Function;
//
//import com.janilla.reflect.Reflection;
//
//public class Evaluator implements Function<String, Object> {
//
//	public static void main(String[] args) {
//		record R(String s) {
//		}
//		var e = new Evaluator();
//		e.setContext(Map.of("s", "foo", "r", new R("bar")));
//
//		var o = e.apply("s");
//		System.out.println(o);
//		assert Objects.equals(o, "foo") : o;
//
//		o = e.apply("r.s");
//		System.out.println(o);
//		assert Objects.equals(o, "bar") : o;
//	}
//
//	protected Object context;
//
//	public Object getContext() {
//		return context;
//	}
//
//	public void setContext(Object context) {
//		this.context = context;
//	}
//
//	@Override
//	public Object apply(String expression) {
//		if (expression.isEmpty())
//			return context;
//		var w = wrap();
//		for (var n : expression.split("\\."))
//			next(n, w);
//		return unwrap(w);
//	}
//
//	protected Object wrap() {
//		return new Object[] { context };
//	}
//
//	protected Object unwrap(Object wrapper) {
//		return ((Object[]) wrapper)[0];
//	}
//
//	protected void next(String name, Object wrapper) {
//		var u = getValue(wrapper);
//		var v = u != null ? switch (u) {
//		case Map<?, ?> m -> m.get(name);
//		case Function<?, ?> f -> {
//			@SuppressWarnings("unchecked")
//			var g = (Function<Object, Object>) f;
//			yield g.apply(name);
//		}
//		default -> {
//			if (u instanceof RenderHelper e) {
//				var o = e.tryToRender(name);
//				if (o != RenderHelper.NOT_RENDERED)
//					yield o;
//			}
//			var g = Reflection.property(u.getClass(), name);
//			yield g != null ? invoke(g, u, wrapper) : null;
//		}
//		} : null;
//		setValue(v, wrapper);
//	}
//
//	protected Object getValue(Object wrapper) {
//		return ((Object[]) wrapper)[0];
//	}
//
//	protected void setValue(Object value, Object wrapper) {
//		((Object[]) wrapper)[0] = value;
//	}
//
//	protected Object invoke(Method method, Object object, Object wrapper) {
//		try {
//			return method.invoke(object);
//		} catch (ReflectiveOperationException e) {
//			throw new RuntimeException(e);
//		}
//	}
//}
