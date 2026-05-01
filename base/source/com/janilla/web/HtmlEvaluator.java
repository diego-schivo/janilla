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
package com.janilla.web;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.java.JavaReflect;
import com.janilla.web.DefaultInvocationResolver.A;

public class HtmlEvaluator {

	public String evaluate(AnnotatedValue input, String expression, Consumer<Object> consumer,
			HtmlRenderer<?> renderer) {
//		IO.println("HtmlEvaluator.evaluate, expression=" + expression);

		if (!expression.isEmpty())
			for (var k : expression.split("\\.")) {
				if (input.value() == null)
					break;
				input = evaluate(input.value(), k);
//				IO.println("Renderer.interpolate, k=" + k + ", input=" + input);
			}

		var a = input.annotated();
		var v = input.value();

		consumer.accept(v);

		var r = v instanceof Renderable<?> x ? x
				: v != null && !(expression.isEmpty() && a == null) ? renderer.renderableFactory.createRenderable(a, v)
						: null;
		var m = v instanceof Iterator || v instanceof Iterable || v instanceof Stream;
		if (r != null) {
			if (r.renderer().templateKey1 == null)
				r.renderer().templateKey1 = renderer.templateKey1;
			if (m && a instanceof AnnotatedParameterizedType x)
				r.renderer().elementType = x.getAnnotatedActualTypeArguments()[0];
			v = r.get();
		} else if (m) {
			var vv = switch (v) {
			case Stream<?> x -> x;
			case Iterable<?> x -> StreamSupport.stream(x.spliterator(), false);
			case Iterator<?> x -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(x, 0), false);
			default -> throw new RuntimeException();
			};
			var d = renderer.annotation != null ? renderer.annotation.delimiter() : null;
			var c = d != null && !d.isEmpty() ? Collectors.joining(d) : Collectors.joining();
			v = vv.map(x -> {
				var r2 = renderer.renderableFactory.createRenderable(renderer.elementType, x);
				if (r2.renderer().templateKey1 == null)
					r2.renderer().templateKey1 = renderer.templateKey1;
				return r2.get();
			}).collect(c);
		}
		return Objects.toString(v, "");
	}

	protected AnnotatedValue evaluate(Object input, String key) {
//		IO.println("HtmlEvaluator.evaluate, input=" + input.getClass() + ", key=" + key);
		AnnotatedElement a;
		Object v;
		switch (input) {
		case Function<?, ?> x:
			a = null;
			@SuppressWarnings("unchecked")
			var f = (Function<String, ?>) x;
			v = f.apply(key);
			break;
		case Map<?, ?> x:
			a = null;
			v = x.get(key);
			break;
		default:
			var p = JavaReflect.property(input.getClass(), key);

			if (p != null && p.member() instanceof Method m) {
				class A {
					private static final Map<Method, Optional<AnnotatedType>> RESULTS = new ConcurrentHashMap<>();
				}
				a = A.RESULTS.computeIfAbsent(m, _ -> {
					var r = p.annotatedType();
					if (r != null && r.getAnnotations().length == 0 && !(r instanceof AnnotatedParameterizedType x
							&& x.getAnnotatedActualTypeArguments()[0].getAnnotations().length != 0))
						r = JavaReflect.inheritedMethods(m)
								.map(x -> x.getReturnType() == Void.TYPE ? x.getAnnotatedParameterTypes()[0]
										: x.getAnnotatedReturnType())
								.filter(x -> x.getAnnotations().length != 0
										|| (x instanceof AnnotatedParameterizedType x2
												&& x2.getAnnotatedActualTypeArguments()[0]
														.getAnnotations().length != 0))
								.findFirst().orElse(null);
					return Optional.ofNullable(r);
				}).orElse(null);
			} else
				a = p != null ? p.annotatedType() : null;

			v = p != null ? p.get(input) : null;
		}
		var av = new AnnotatedValue(a, v);
//		IO.println("HtmlEvaluator.evaluate, av=" + av);
		return av;
	}
}
