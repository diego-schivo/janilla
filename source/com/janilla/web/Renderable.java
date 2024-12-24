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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;

public record Renderable<T>(T value, Renderer<T> renderer) {

	protected static Map<String, Map<String, String>> allTemplates = new HashMap<>();

	public static Pattern templateElement = Pattern.compile("<template id=\"([\\w-]+)\">(.*?)</template>",
			Pattern.DOTALL);

	public static <T> Renderable<T> of(AnnotatedElement annotated, T value) {
		var r = Stream.of(annotated, value != null ? value.getClass() : null)
				.map(x -> x != null ? x.getAnnotation(Render.class) : null).filter(x -> x != null).findFirst()
				.map(x -> {
					@SuppressWarnings("unchecked")
					var c = (Class<Renderer<T>>) x.renderer();
					Renderer<T> i;
					try {
						i = c.getConstructor().newInstance();
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					i.templateName = x.template();
					if (i.templateName.endsWith(".html")) {
						i.templates = allTemplates.computeIfAbsent(i.templateName, k -> {
							var m = new HashMap<String, String>();
							try (var is = value.getClass().getResourceAsStream(k)) {
								if (is == null)
									throw new NullPointerException();
								var s = new String(is.readAllBytes());
								var v = templateElement.matcher(s).replaceAll(y -> {
									m.put(y.group(1), y.group(2));
									return "";
								});
								m.put(null, v);
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
							return m;
						});
					}
					return i;
				}).orElse(null);
		if (r == null && value instanceof List && annotated instanceof AnnotatedParameterizedType apt) {
			var at = apt.getAnnotatedActualTypeArguments()[0];
			@SuppressWarnings("unchecked")
			var r2 = (Renderer<T>) new ListRenderer<>(at);
			r = r2;
		}
		return new Renderable<>(value, r);
	}

	public String render(HttpExchange exchange) {
		return renderer.apply(value, exchange);
	}

	public static class ListRenderer<T> extends Renderer<List<T>> {

		protected final AnnotatedType annotatedType;

		public ListRenderer(AnnotatedType annotatedType) {
			this.annotatedType = annotatedType;
		}

		@Override
		public String apply(List<T> value, HttpExchange exchange) {
			return value.stream().map(x -> {
				var r = Renderable.of(annotatedType, x);
				if (r.renderer().templates == null)
					r.renderer().templates = templates;
				return r.render(null);
			}).collect(Collectors.joining());
		}
	}
}
