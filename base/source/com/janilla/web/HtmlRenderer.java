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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.java.Reflection;

public class HtmlRenderer<T> extends Renderer<T> {

	protected static final Pattern ATTRIBUTE_COMMENT_TEXT = Pattern.compile(
			String.join("|", "[\\w-]+=\"([^\"]*?\\$\\{.*?\\}.*?)\"", "<!--(\\$\\{.*?\\})-->", "(\\$\\{.*?\\})"));

	protected static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	public String apply(T value) {
		var t = template(value);
		var s = ATTRIBUTE_COMMENT_TEXT.matcher(t).replaceAll(x -> {
			var i = IntStream.rangeClosed(1, x.groupCount()).filter(y -> x.group(y) != null).findFirst().getAsInt();
			var av = new AnnotatedValue(null, value);
			var vv = new ArrayList<>();
			var s2 = PLACEHOLDER.matcher(x.group(i))
					.replaceAll(y -> Matcher.quoteReplacement(string(av, y.group(1), vv::add)));
			if (i == 1) {
				var g = x.group();
				var g1 = x.group(1);
				if (vv.size() == 1 && vv.getFirst() instanceof Boolean b) {
					if (!b)
						return "";
					if (g1.startsWith("${") && g1.indexOf("}") == g1.length() - 1)
						s2 = "";
				}
				var o = x.start(1) - x.start();
				s2 = g.substring(0, o) + s2 + g.substring(o + g1.length());
			}
//			IO.println("Renderer.interpolate, s=" + s);
			return Matcher.quoteReplacement(s2);
		});
		return s;
	}

	protected String template(T value) {
		var t = "${}";
		if (annotation != null && !annotation.template().isEmpty()) {
			var k1 = annotation.template();
			var k2 = "";
			if (!annotation.template().endsWith(".html")) {
				k1 = templateKey1;
				k2 = annotation.template();
			}
			t = renderableFactory.template(k1, k2);
			if (t == null)
				throw new NullPointerException(k1 + ", " + k2);
		}
		return t;
	}

	protected String string(AnnotatedValue input, String expression, Consumer<Object> consumer) {
		if (!expression.isEmpty())
			for (var k : expression.split("\\.")) {
				if (input.value() == null)
					break;
				input = annotatedValue(input.value(), k);
//						IO.println("Renderer.interpolate, k=" + k + ", av=" + av);
			}
		var v = input.value();
		consumer.accept(v);
		var r = v instanceof Renderable<?> x ? x
				: v != null && !(expression.isEmpty() && input.annotated() == null)
						? renderableFactory.createRenderable(input.annotated(), v)
						: null;
		var m = v instanceof Iterator || v instanceof Iterable || v instanceof Stream;
		if (r != null) {
			if (r.renderer().templateKey1 == null)
				r.renderer().templateKey1 = templateKey1;
			if (m && input.annotated() instanceof AnnotatedParameterizedType x)
				r.renderer().elementType = x.getAnnotatedActualTypeArguments()[0];
			v = r.get();
		} else if (m) {
			var vv = switch (v) {
			case Stream<?> x -> x;
			case Iterable<?> x -> StreamSupport.stream(x.spliterator(), false);
			case Iterator<?> x -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(x, 0), false);
			default -> throw new RuntimeException();
			};
			var d = annotation != null ? annotation.delimiter() : null;
			var c = d != null && !d.isEmpty() ? Collectors.joining(d) : Collectors.joining();
			v = vv.map(x -> {
				var r2 = renderableFactory.createRenderable(elementType, x);
				if (r2.renderer().templateKey1 == null)
					r2.renderer().templateKey1 = templateKey1;
				return r2.get();
			}).collect(c);
		}
		return Objects.toString(v, "");
	}

	protected AnnotatedValue annotatedValue(Object input, String key) {
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
			var p = Reflection.property(input.getClass(), key);
			a = p != null ? p.annotatedType() : null;
			v = p != null ? p.get(input) : null;
		}
		return new AnnotatedValue(a, v);
	}
}
