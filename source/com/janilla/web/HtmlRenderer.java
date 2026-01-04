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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.reflect.Reflection;

public class HtmlRenderer<T> extends Renderer<T> {

	protected static final Pattern COMMENT = Pattern.compile("<!--(\\$\\{.*?\\})-->");

	protected static final Pattern ATTRIBUTE = Pattern.compile("[\\w-]+=\"([^\"]*?\\$\\{.*?\\}.*?)\"");

	protected static final Pattern TEXT = Pattern.compile("(\\$\\{.*?\\})");

	protected static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	public String apply(T value) {
		var t = Objects.requireNonNullElse(template(value), "${}");
		for (var i = 0; i < 3; i++) {
			var p = switch (i) {
			case 0 -> COMMENT;
			case 1 -> ATTRIBUTE;
			case 2 -> TEXT;
			default -> throw new IndexOutOfBoundsException(i);
			};
			record A(MatchResult mr, Object[] vv) {
			}
			BiFunction<String, A, String> f = switch (i) {
			case 0, 2 -> (s, _) -> s;
			case 1 -> (s, a) -> {
				var g = a.mr.group();
				var g1 = a.mr.group(1);
				if (a.vv.length == 1 && a.vv[0] instanceof Boolean b) {
					if (!b)
						return "";
					if (g1.startsWith("${") && g1.indexOf("}") == g1.length() - 1)
						s = "";
				}
				var o = a.mr.start(1) - a.mr.start();
				return g.substring(0, o) + s + g.substring(o + g1.length());
			};
			default -> throw new RuntimeException();
			};
			t = p.matcher(t).replaceAll(x -> {
				var gs = x.group(1);
				var vv = new ArrayList<>();
				var s = PLACEHOLDER.matcher(gs).replaceAll(y -> {
					var e = y.group(1);
					var av = new AnnotatedValue(null, value);
					if (!e.isEmpty())
						for (var k : e.split("\\.")) {
							if (av.value() == null)
								break;
							av = evaluate(av, k);
//							IO.println("Renderer.interpolate, k=" + k + ", av=" + av);
//							if (k.equals("selected"))
//								IO.println("debugger");
						}
					var v = av.value();
					vv.add(v);
					var r = v instanceof Renderable<?> z ? z
							: v != null && !(e.isEmpty() && av.annotated() == null)
									? renderableFactory.createRenderable(av.annotated(), v)
									: null;
					var iis = v instanceof Iterator || v instanceof Iterable || v instanceof Stream;
					if (r != null) {
						if (r.renderer().templateKey1 == null)
							r.renderer().templateKey1 = templateKey1;
						if (iis && av.annotated() instanceof AnnotatedParameterizedType apt)
							r.renderer().elementType = apt.getAnnotatedActualTypeArguments()[0];
						v = r.get();
					} else if (iis) {
						var w = switch (v) {
						case Stream<?> z -> z;
						case Iterable<?> z -> StreamSupport.stream(z.spliterator(), false);
						case Iterator<?> z -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(z, 0), false);
						default -> throw new RuntimeException();
						};
						var d = annotation != null ? annotation.delimiter() : null;
						var c = d != null && !d.isEmpty() ? Collectors.joining(d) : Collectors.joining();
						v = w.map(z -> {
							var r2 = renderableFactory.createRenderable(elementType, z);
							if (r2.renderer().templateKey1 == null)
								r2.renderer().templateKey1 = templateKey1;
							return r2.get();
						}).collect(c);
					}
					return Matcher.quoteReplacement(Objects.toString(v, ""));
				});
				s = f.apply(s, new A(x, vv.toArray()));
//				IO.println("Renderer.interpolate, s=" + s);
				return Matcher.quoteReplacement(s);
			});
		}
		return t;
	}

	protected String template(T value) {
		var k = annotation != null ? annotation.template() : null;
		if (k == null || k.isEmpty())
			return null;
		String k1, k2;
		if (k.endsWith(".html")) {
			k1 = k;
			k2 = "";
		} else {
			k1 = templateKey1;
			k2 = k;
		}
		var t = renderableFactory.template(k1, k2);
		if (t == null)
			throw new NullPointerException(k1 + ", " + k2);
		return t;
	}

	protected AnnotatedValue evaluate(AnnotatedValue input, String key) {
		AnnotatedElement ae;
		Object v;
		switch (input.value()) {
		case Function<?, ?> y:
			ae = null;
			@SuppressWarnings("unchecked")
			var f = (Function<String, ?>) y;
			v = f.apply(key);
			break;
		case Map<?, ?> y:
			ae = null;
			v = y.get(key);
			break;
		default:
			var p = Reflection.property(input.value().getClass(), key);
			ae = p != null ? p.annotatedType() : null;
			v = p != null ? p.get(input.value()) : null;
		}
		return new AnnotatedValue(ae, v);
	}
}
