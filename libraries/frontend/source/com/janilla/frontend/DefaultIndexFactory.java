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
import com.janilla.ioc.DiFactory;
import com.janilla.web.JavaResource;
import com.janilla.web.ResourceMap;

public class DefaultIndexFactory<C extends FrontendConfig> implements IndexFactory {

	protected final C config;

	protected final DiFactory diFactory;

	protected final ResourceMap resourceMap;

	protected Map<String, String> imports;

	protected List<Script> scripts;

	protected List<Template> templates;

	public DefaultIndexFactory(C config, ResourceMap resourceMap, DiFactory diFactory) {
		this.config = config;
		this.resourceMap = resourceMap;
		this.diFactory = diFactory;
	}

	@Override
	public Index newIndex() {
		var aa = new HashMap<String, Object>();
		putIndexInitArgs(aa);
		var i = diFactory.newInstance(diFactory.classFor(Index.class), aa);
//		IO.println("DefaultIndexFactory.newIndex, i=" + i);

		return i;
	}

	protected void putIndexInitArgs(Map<String, Object> args) {
		args.put("app", newApp());
		args.put("imports", imports());
		args.put("basePath", config.basePath());
		args.put("scripts", scripts());
		args.put("templates", templates());
		args.put("title", config.title());
	}

	protected App newApp() {
		var aa = new HashMap<String, Object>();
		putAppInitArgs(aa);
		var a = diFactory.newInstance(diFactory.classFor(App.class), aa);
//		IO.println("DefaultIndexFactory.newApp, a=" + a);

		return a;
	}

	protected void putAppInitArgs(Map<String, Object> args) {
		args.put("env", env());
		args.put("state", state());
	}

	protected Map<String, String> env() {
		var m = new LinkedHashMap<String, String>();
		m.put("apiUrl", config.api().url());
		m.put("basePath", config.basePath());

		return m;
	}

	protected Map<String, Object> state() {
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
		Stream.of("app", "intl-format", "janilla-logo", "lucide-icon", "toaster", "web-component")
				.map(this::baseImportKey).forEach(x -> map.put(x, config.basePath() + "/" + x + ".js"));
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
		var f = (JavaResource) resourceMap.get("/" + name + ".html");
		try (var s = f != null ? f.newInputStream() : null) {
			return s != null ? new Template(name, new String(s.readAllBytes())) : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
