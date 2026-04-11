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
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.ioc.DiFactory;
import com.janilla.java.AnnotationAndElement;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;

public class DefaultRenderableFactory implements RenderableFactory {

	protected static final Pattern TEMPLATE_ELEMENT = Pattern
			.compile("<ssr-template id=\"([\\w-]+)\">(.*?)</ssr-template>", Pattern.DOTALL);

	protected final DiFactory diFactory;

	protected final ResourceMap resourceMap;

	protected final Map<String, Map<String, String>> templates = new ConcurrentHashMap<>();

	public DefaultRenderableFactory(ResourceMap resourceMap) {
		this(resourceMap, null);
	}

	public DefaultRenderableFactory(ResourceMap resourceMap, DiFactory diFactory) {
		this.resourceMap = resourceMap;
		this.diFactory = diFactory;
	}

	@Override
	public String template(String key1, String key2) {
//		IO.println("RenderableFactory.template, key1=" + key1 + ", key2=" + key2);
		return Optional.ofNullable(templates.get(key1)).map(x -> x.get(key2)).orElse(null);
	}

	@Override
	public <T> Renderable<T> createRenderable(AnnotatedElement annotated, T value) {
//		IO.println("RenderableFactory.createRenderable, annotated=" + annotated + ", value=" + value);
		var ae = Stream.of(value != null ? value.getClass() : null, annotated).filter(x -> x != null).map(x -> {
			var r = x.getAnnotation(Render.class);
			if (r != null)
				return new AnnotationAndElement<>(r, x);
			var c = x instanceof AnnotatedType at ? Java.toClass(at.getType()) : (Class<?>) x;
			return JavaReflect.inheritedAnnotation(c, Render.class);
		}).filter(x -> x != null).findFirst().orElse(null);
//		IO.println("RenderableFactory.createRenderable, ae=" + ae);

		@SuppressWarnings("unchecked")
		var c = (Class<Renderer<T>>) (ae != null ? ae.annotation().renderer() : HtmlRenderer.class);
		var r = createRenderer(c);
		r.renderableFactory = this;
		r.annotation = ae != null ? ae.annotation() : null;
		if (ae != null && ae.annotation().resource().length != 0) {
			var t = ae.annotation().template();
			templates.computeIfAbsent(t, _ -> {
				var s = resourceKeys(ae).map(x -> {
					var r2 = resourceMap.get(x);
					if (r2 instanceof DefaultResource r3)
						try (var in = r3.newInputStream()) {
							if (in == null)
								throw new NullPointerException(t);
							return new String(in.readAllBytes());
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					else
						throw new NullPointerException(x);
				}).collect(Collectors.joining());

				var m = new LinkedHashMap<String, String>();
				m.put("", TEMPLATE_ELEMENT.matcher(s).replaceAll(x -> {
					m.put(x.group(1), x.group(2));
					return "";
				}));
//				IO.println("RenderableFactory.createRenderable, t=" + t + ", m.keySet()=" + m.keySet());
				return m;
			});
			r.templateKey1 = t;
		}
		return new Renderable<>(value, r);
	}

	protected <T> Renderer<T> createRenderer(Class<Renderer<T>> class1) {
//		IO.println("RenderableFactory.createRenderer, class1=" + class1);
		var x = diFactory != null ? diFactory.newInstance(class1) : null;
		try {
			return x != null ? x : class1.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("class1=" + class1, e);
		}
	}

	protected Stream<String> resourceKeys(AnnotationAndElement<Render> render) {
		return Arrays.stream(render.annotation().resource());
	}
}
