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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RenderableFactory {

	protected Map<String, Map<String, String>> allTemplates = new ConcurrentHashMap<>();

	protected static final Pattern TEMPLATE_ELEMENT = Pattern.compile("<template id=\"([\\w-]+)\">(.*?)</template>",
			Pattern.DOTALL);

	public <T> Renderable<T> createRenderable(AnnotatedElement annotated, T value) {
//		System.out.println("MethodHandlerFactory.of, annotated=" + annotated + ", value=" + value);
		var ra = Stream.of(annotated, value != null ? value.getClass() : null)
				.map(x -> x != null ? x.getAnnotation(Render.class) : null).filter(x -> x != null).findFirst()
				.orElse(null);
		@SuppressWarnings("unchecked")
		var rc = ra != null ? (Class<Renderer<T>>) ra.renderer() : null;
		Renderer<T> r = null;
		if (rc != null) {
			try {
				r = rc.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			r.factory = this;
			r.annotation = ra;
			if (ra.template().endsWith(".html")) {
				r.templates = allTemplates.computeIfAbsent(ra.template(), k -> {
					var m = new ConcurrentHashMap<String, String>();
					try (var is = value.getClass().getResourceAsStream(k)) {
						if (is == null)
							throw new NullPointerException(k);
						var s = new String(is.readAllBytes());
						var v = TEMPLATE_ELEMENT.matcher(s).replaceAll(y -> {
							m.put(y.group(1), y.group(2));
							return "";
						});
						m.put("", v);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					return m;
				});
			}
		}
		return new Renderable<>(value, r);
	}
}
