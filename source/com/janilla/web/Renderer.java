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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com.janilla.http.HttpExchange;
import com.janilla.reflect.Reflection;

public class Renderer<T> implements BiFunction<T, HttpExchange, String> {

	protected static Pattern placeholder = Pattern.compile(
			">\\$\\{([\\w.-]*)\\}</" + "|" + "<!--\\$\\{([\\w.-]*)\\}-->" + "|" + "[\\w-]+=\"\\$\\{([\\w.-]*)\\}\"");

	public static Map<String, Object> merge(Object... objects) {
		var m = new LinkedHashMap<String, Object>();
		for (var o : objects)
			switch (o) {
			case Map<?, ?> x:
				@SuppressWarnings("unchecked")
				var y = (Map<String, Object>) x;
				m.putAll(y);
				break;
			default:
				Reflection.properties(o.getClass()).forEach(x -> m.put(x.name(), x.get(o)));
				break;
			}
		return m;
	}

	protected Map<String, String> templates;

	protected String templateName;

	public void setTemplates(Map<String, String> templates) {
		this.templates = templates;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@Override
	public String apply(T value, HttpExchange exchange) {
		return interpolate(templates.get(templateName.endsWith(".html") ? null : templateName), value);
	}

	protected String interpolate(String template, Object value) {
		return placeholder.matcher(template).replaceAll(x -> {
			var i = 1;
			AnnotatedElement ae = null;
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
						switch (v) {
						case Map<?, ?> z:
							ae = null;
							v = z.get(k);
							break;
						default:
							var p = Reflection.property(v.getClass(), k);
							ae = p != null ? p.annotatedType() : null;
							v = p != null ? p.get(v) : null;
							break;
						}
//						System.out.println("k=" + k + ", ae=" + ae + ", v=" + v);
					}
				break;
			}
			var r = v instanceof Renderable<?> y ? y : v != null ? Renderable.of(ae, v) : null;
			if (r != null && r.renderer() != null) {
				if (r.renderer().templates == null)
					r.renderer().templates = templates;
				v = r.render(null);
			}
			return switch (i) {
			case 1 -> ">" + (v != null ? v : "") + "</";
			case 2 -> v != null ? v.toString() : "";
			case 3 -> Boolean.FALSE.equals(v) ? ""
					: x.group().substring(0, x.group().indexOf('='))
							+ (Boolean.TRUE.equals(v) ? "" : ("=\"" + (v != null ? v : "") + "\""));
			default -> throw new RuntimeException();
			};
		});
	}
}
