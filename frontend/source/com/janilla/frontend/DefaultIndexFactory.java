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
package com.janilla.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.web.DefaultResource;
import com.janilla.web.ResourceMap;

public class DefaultIndexFactory implements IndexFactory {

	protected final ResourceMap resourceMap;

	protected Map<String, String> imports;

	protected List<Script> scripts;

	protected List<Template> templates;

	public DefaultIndexFactory(ResourceMap resourceMap) {
		this.resourceMap = resourceMap;
	}

	@Override
	public Index newIndex(HttpExchange exchange) {
		return new SimpleIndex(null, imports(), scripts(), new SimpleApp(null, state(exchange)), templates());
	}

	protected Map<String, Object> state(HttpExchange exchange) {
		return new LinkedHashMap<String, Object>();
	}

	protected Map<String, String> imports() {
		if (imports == null)
			synchronized (this) {
				if (imports == null) {
					imports = new LinkedHashMap<String, String>();
					putImports(imports);
				}
			}
		return imports;
	}

	protected void putImports(Map<String, String> map) {
		Stream.of("app", "intl-format", "janilla-logo", "toaster", "web-component").map(this::baseImportKey)
				.forEach(x -> map.put(x, "/" + x + ".js"));
	}

	protected String baseImportKey(String name) {
		return name;
	}

	protected List<Script> scripts() {
		return new ArrayList<>();
	}

	protected List<Template> templates() {
		if (templates == null)
			synchronized (this) {
				if (templates == null) {
					templates = new ArrayList<Template>();
					addTemplates(templates);
				}
			}
		return templates;
	}

	protected void addTemplates(List<Template> list) {
	}

	protected Template template(String name) {
		var f = (DefaultResource) resourceMap.get("/" + name + ".html");
		try (var in = f != null ? f.newInputStream() : null) {
			return in != null ? new Template(name, new String(in.readAllBytes())) : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
