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
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;

public record Renderable<T>(T value, Renderer<T> renderer) {

	public static <T> Renderable<T> of(T value) {
		return of(value, null);
	}

	public static <T> Renderable<T> of(T value, AnnotatedElement annotated) {
		return Stream.of(annotated, value != null ? value.getClass() : null)
				.map(x -> x != null ? x.getAnnotation(Render.class) : null).filter(x -> x != null).findFirst()
				.map(x -> {
					@SuppressWarnings("unchecked")
					var c = (Class<Renderer<T>>) x.value();
					Renderer<T> r;
					try {
						r = c.getConstructor().newInstance();
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					return new Renderable<>(value, r);
				}).orElse(null);
	}

	public String render(HttpExchange exchange) {
		return renderer.apply(value, exchange);
	}
}
