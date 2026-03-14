/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

public abstract class AbstractIndexFactory implements IndexFactory {

	protected final ResourceMap resourceMap;

	protected Map<String, String> imports;

	protected List<Script> scripts;

	protected List<Template> templates;

	protected AbstractIndexFactory(ResourceMap resourceMap) {
		this.resourceMap = resourceMap;
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
//		if (scripts == null)
//			synchronized (this) {
//				if (scripts == null) {
//					scripts = new ArrayList<Script>();
//					addScripts(scripts);
//				}
//			}
//		return scripts;
		return new ArrayList<>();
	}

//	protected void addScripts(List<Script> list) {
//	}

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
