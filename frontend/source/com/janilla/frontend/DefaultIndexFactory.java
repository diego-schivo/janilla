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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.web.DefaultResource;
import com.janilla.web.ResourceMap;

public class DefaultIndexFactory<C extends FrontendConfig> implements IndexFactory {

	protected final C config;

	protected final DiFactory diFactory;

	protected final ResourceMap resourceMap;

	protected Map<String, String> imports;

	protected List<Script> scripts;

	protected List<Template> templates;

	public DefaultIndexFactory(C config, ResourceMap resourceMap) {
		this(config, resourceMap, null);
	}

	public DefaultIndexFactory(C config, ResourceMap resourceMap, DiFactory diFactory) {
		this.config = config;
		this.resourceMap = resourceMap;
		this.diFactory = diFactory;
	}

	@Override
	public Index newIndex(HttpExchange exchange) {
		var a = newApp(exchange);
		Index i;
		if (diFactory != null) {
			var aa = new HashMap<String, Object>();
			putIndexInitArgs(aa, exchange);
			i = diFactory.newInstance(diFactory.classFor(Index.class), aa);
		} else
			i = new DefaultIndex(config.title(), imports(), scripts(), a, templates());
//		IO.println("DefaultIndexFactory.newIndex, i=" + i);
		return i;
	}

	protected void putIndexInitArgs(Map<String, Object> args, HttpExchange exchange) {
		args.put("title", config.title());
		args.put("imports", imports());
		args.put("scripts", scripts());
		args.put("app", newApp(exchange));
		args.put("templates", templates());
	}

	protected App newApp(HttpExchange exchange) {
		App a;
		if (diFactory != null) {
			var aa = new HashMap<String, Object>();
			putAppInitArgs(aa, exchange);
			a = diFactory.newInstance(diFactory.classFor(App.class), aa);
		} else
			a = new DefaultApp(config.api().url(), state(exchange));
//		IO.println("DefaultIndexFactory.newApp, a=" + a);
		return a;
	}

	protected void putAppInitArgs(Map<String, Object> args, HttpExchange exchange) {
		args.put("apiUrl", config.api().url());
		args.put("state", state(exchange));
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
		return "base/" + name;
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
