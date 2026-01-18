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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.janilla.ioc.DiFactory;

public class RenderableFactory {

	protected static final Pattern TEMPLATE_ELEMENT = Pattern.compile("<ssr-template id=\"([\\w-]+)\">(.*?)</ssr-template>",
			Pattern.DOTALL);

	protected final Map<String, Map<String, String>> templates = new ConcurrentHashMap<>();

	protected final DiFactory diFactory;

	public RenderableFactory() {
		this(null);
	}

	public RenderableFactory(DiFactory diFactory) {
		this.diFactory = diFactory;
	}

	public String template(String key1, String key2) {
		return Optional.ofNullable(templates.get(key1)).map(x -> x.get(key2)).orElse(null);
	}

	public <T> Renderable<T> createRenderable(AnnotatedElement annotated, T value) {
//		IO.println("RenderableFactory.createRenderable, annotated=" + annotated + ", value=" + value);
		var a = annotation(annotated, value);
		@SuppressWarnings("unchecked")
		var c = (Class<Renderer<T>>) (a != null ? a.renderer() : HtmlRenderer.class);
		var r = createRenderer(c);
		r.renderableFactory = this;
		r.annotation = a;
		var t = a != null ? a.template() : null;
		if (t != null && t.endsWith(".html")) {
			templates.computeIfAbsent(t, k -> {
				var m = new ConcurrentHashMap<String, String>();
				try (var in = getResourceAsStream(value, k)) {
					if (in == null)
						throw new NullPointerException(k);
					var s = new String(in.readAllBytes());
					m.put("", TEMPLATE_ELEMENT.matcher(s).replaceAll(x -> {
						m.put(x.group(1), x.group(2));
						return "";
					}));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return m;
			});
			r.templateKey1 = t;
		}
		return new Renderable<>(value, r);
	}

	protected <T> Renderer<T> createRenderer(Class<Renderer<T>> c) {
		var x = diFactory != null ? diFactory.create(c) : null;
		try {
			return x != null ? x : c.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("c=" + c, e);
		}
	}

	protected <T> Render annotation(AnnotatedElement annotated, T value) {
		return Stream.of(annotated, value != null ? value.getClass() : null)
				.map(x -> x != null ? x.getAnnotation(Render.class) : null).filter(Objects::nonNull).findFirst()
				.orElse(null);
	}

	protected <T> InputStream getResourceAsStream(T value, String name) {
		return value.getClass().getResourceAsStream(name);
	}
}
