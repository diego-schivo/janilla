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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com.janilla.http.HttpExchange;
import com.janilla.reflect.Reflection;

public abstract class Renderer<T> implements BiFunction<T, HttpExchange, String> {

	protected static Map<String, Map<String, String>> templates = new HashMap<>();

	public static Pattern template = Pattern.compile("<template id=\"([\\w-]+)\">(.*?)</template>", Pattern.DOTALL);

	protected static Pattern textCommentAttribute = Pattern.compile(
			">\\$\\{([\\w.-]*)\\}</" + "|" + "<!--\\$\\{([\\w.-]*)\\}-->" + "|" + "[\\w-]+=\"\\$\\{([\\w.-]*)\\}\"");

	protected static <T> String interpolate(String template, T value) {
		return textCommentAttribute.matcher(template).replaceAll(x -> {
			var i = 1;
			Object v = null;
			for (; i <= 3; i++) {
				var e = x.group(i);
				if (e == null)
					continue;
				v = value;
				if (!e.isEmpty())
					for (var k : e.split("\\.")) {
						if (v == null)
							break;
						v = switch (v) {
						case Map<?, ?> z -> z.get(k);
						default -> {
							var p = Reflection.property(v.getClass(), k);
							yield p != null ? p.get(v) : null;
						}
						};
					}
				break;
			}
			var r = Renderable.of(v);
			if (r != null)
				v = r.render(null);
			return switch (i) {
			case 1 -> ">" + (v != null ? v : "") + "</";
			case 2 -> v != null ? v.toString() : "";
			case 3 -> v != null && !Boolean.FALSE.equals(v)
					? x.group().substring(0, x.group().indexOf('=')) + "=\"" + (v != null ? v : "") + "\""
					: "";
			default -> throw new RuntimeException();
			};
		});
	}

	protected static Map<String, Object> merge(Object... objects) {
		var m = new LinkedHashMap<String, Object>();
		for (var o : objects)
			switch (o) {
			case Map<?, ?> x:
				@SuppressWarnings("unchecked")
				var y = (Map<String, Object>) x;
				m.putAll(y);
				break;
			default:
				Reflection.properties2(o.getClass()).forEach(x -> m.put(x.getName(), x.get(o)));
				break;
			}
		return m;
	}

	protected Map<String, String> templates(String name) {
		var tt = templates.computeIfAbsent(name, k -> {
			var m = new HashMap<String, String>();
			try (var is = getClass().getResourceAsStream(k)) {
				if (is == null)
					throw new NullPointerException();
				var s = new String(is.readAllBytes());
				var v = template.matcher(s).replaceAll(y -> {
					m.put(y.group(1), y.group(2));
					return "";
				});
				m.put(null, v);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return m;
		});
		return tt;
	}
}
