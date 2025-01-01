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

	protected static final Pattern SEARCH_HTML = Pattern.compile("<!--(\\$\\{.*?\\})-->" + "|"
			+ "[\\w-]+=\"([^\"]*?\\$\\{.*?\\}.*?)\"" + "|" + "([^<>]*?\\$\\{.*?\\}[^<>]*)");

	protected static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	protected RenderableFactory factory;

	protected Render annotation;

	protected Map<String, String> templates;

	@Override
	public String apply(T value) {
		var t = template(value);
		if (t == null)
			throw new NullPointerException(Objects.toString(value));
		return SEARCH_HTML.matcher(t).replaceAll(x -> {
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
					}
				var v = av.value;
				vv.add(v);
				var r = v instanceof Renderable<?> z ? z : v != null ? factory.createRenderable(av.annotated, v) : null;
				var iis = v instanceof Iterator || v instanceof Iterable || v instanceof Stream;
				if (r != null && r.renderer() != null) {
//					if (iis && av.annotated instanceof AnnotatedParameterizedType apt) {
//						var at = apt.getAnnotatedActualTypeArguments()[0];
//						var a = at.getAnnotation(Render.class);
//						r2.delimiter = a != null ? a.delimiter() : null;
//					}
					if (r.renderer().templates == null)
						r.renderer().templates = templates;
					v = r.get();
				} else if (iis) {
					var w = switch (v) {
					case Stream<?> z -> z;
					case Iterable<?> z -> StreamSupport.stream(z.spliterator(), false);
					case Iterator<?> z -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(z, 0), false);
					default -> throw new RuntimeException();
					};
					var c = annotation.delimiter() != null && !annotation.delimiter().isEmpty()
							? Collectors.joining(annotation.delimiter())
							: Collectors.joining();
					v = w.map(z -> {
						var r2 = factory.createRenderable(null, z);
						if (r2.renderer() != null && r2.renderer().templates == null)
							r2.renderer().templates = templates;
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
		return templates.get(annotation.template().endsWith(".html") ? "" : annotation.template());
	}

	protected AnnotatedValue evaluate(AnnotatedValue x, String k) {
		switch (x.value) {
		case Function<?, ?> y:
			@SuppressWarnings("unchecked")
			var f = (Function<String, ?>) y;
			return new AnnotatedValue(null, f.apply(k));
		case Map<?, ?> y:
			return new AnnotatedValue(null, y.get(k));
		default:
			var p = Reflection.property(x.value.getClass(), k);
			return new AnnotatedValue(p != null ? p.annotatedType() : null, p != null ? p.get(x.value) : null);
		}
	}

	public record AnnotatedValue(AnnotatedElement annotated, Object value) {
	}
}
