/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.conduit.frontend;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontendConfig;
import com.janilla.blanktemplate.frontend.BlankIndexFactory;
import com.janilla.frontend.ApiClient;
import com.janilla.frontend.Template;
import com.janilla.ioc.DiFactory;
import com.janilla.web.ResourceMap;

class IndexFactoryImpl extends BlankIndexFactory<BlankFrontendConfig> {

	public IndexFactoryImpl(BlankFrontendConfig config, ResourceMap resourceMap, DiFactory diFactory,
			ApiClient apiClient) {
		super(config, resourceMap, diFactory, apiClient);
	}

	@Override
	public Template blankTemplate(String name) {
		return template("blank/" + name);
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);

		Stream.of("app", "article", "article-preview", "articles", "comments", "editor", "errors", "favorite-button",
				"follow-button", "home", "login", "nav-link", "page-display", "pagination-nav", "popular-tags",
				"profile", "register", "settings", "tags-input")
				.forEach(x -> map.put(x, config.basePath() + "/" + x + ".js"));
	}

	@Override
	protected String blankImportKey(String name) {
		return "blank/" + name;
	}

	@Override
	protected void addTemplates(List<Template> list) {
		super.addTemplates(list);

		Stream.of("app", "article", "article-preview", "articles", "comments", "editor", "errors", "favorite-button",
				"follow-button", "home", "login", "nav-link", "page-display", "pagination-nav", "popular-tags",
				"profile", "register", "settings", "tags-input").map(this::template).forEach(list::add);
	}
}
