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
package com.janilla.web;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.reflect.Reflection;

public class Renderer<T> implements Function<T, String> {

	protected static final Pattern HTML_NODE = Pattern.compile("<!--(\\$\\{.*?\\})-->" + "|"
			+ "[\\w-]+=\"([^\"]*?\\$\\{.*?\\}.*?)\"" + "|" + "([^<>]*?\\$\\{.*?\\}[^<>]*)");

	protected static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	protected RenderableFactory factory;

	protected Render annotation;

	protected String templateKey1;

	protected AnnotatedType elementType;

	@Override
	public String apply(T value) {
		var t = Objects.toString(template(value), "${}");
		return HTML_NODE.matcher(t).replaceAll(x -> {
			var g = IntStream.rangeClosed(1, 3).filter(y -> x.group(y) != null).findFirst().getAsInt();
			var gs = x.group(g);
			var vv = new ArrayList<>();
			var s = PLACEHOLDER.matcher(gs).replaceAll(y -> {
				var e = y.group(1);
				var av = new AnnotatedValue(null, value);
				if (!e.isEmpty())
					for (var k : e.split("\\.")) {
						if (av.value == null)
							break;
						av = evaluate(av, k);
//						System.out.println("Renderer.interpolate, k=" + k + ", av=" + av);
//						if (k.equals("errors"))
//							System.out.println("debugger");
					}
				var v = av.value;
				vv.add(v);
				var r = v instanceof Renderable<?> z ? z
						: v != null && !(e.isEmpty() && av.annotated == null)
								? factory.createRenderable(av.annotated, v)
								: null;
				var iis = v instanceof Iterator || v instanceof Iterable || v instanceof Stream;
				if (r != null) {
					if (r.renderer().templateKey1 == null)
						r.renderer().templateKey1 = templateKey1;
					if (iis && av.annotated instanceof AnnotatedParameterizedType apt)
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
						var r2 = factory.createRenderable(elementType, z);
						if (r2.renderer().templateKey1 == null)
							r2.renderer().templateKey1 = templateKey1;
						return r2.get();
					}).collect(c);
				}
				return Objects.toString(v, "");
			});
			s = switch (g) {
			case 1 -> s;
			case 2 -> {
				if (vv.size() == 1 && vv.get(0) instanceof Boolean b) {
					if (!b)
						yield "";
					if (gs.startsWith("${") && gs.indexOf("}") == gs.length() - 1)
						s = "";
				}
				yield t.substring(x.start(), x.start(g)) + s + t.substring(x.end(g), x.end());
			}
			case 3 -> t.substring(x.start(), x.start(g)) + s + t.substring(x.end(g), x.end());
			default -> throw new RuntimeException();
			};
//			System.out.println("Renderer.interpolate, s=" + s);
			return s;
		});
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
		var t = factory.template(k1, k2);
		if (t == null) {
			throw new NullPointerException(k1 + ", " + k2);
		}
		return t;
	}

	protected AnnotatedValue evaluate(AnnotatedValue x, String k) {
		AnnotatedElement ae;
		Object v;
		switch (x.value) {
		case Function<?, ?> y:
			ae = null;
			@SuppressWarnings("unchecked")
			var f = (Function<String, ?>) y;
			v = f.apply(k);
			break;
		case Map<?, ?> y:
			ae = null;
			v = y.get(k);
			break;
		default:
			var p = Reflection.property(x.value.getClass(), k);
			ae = p != null ? p.annotatedType() : null;
			v = p != null ? p.get(x.value) : null;
		}
		return new AnnotatedValue(ae, v);
	}

	public record AnnotatedValue(AnnotatedElement annotated, Object value) {
	}
}
