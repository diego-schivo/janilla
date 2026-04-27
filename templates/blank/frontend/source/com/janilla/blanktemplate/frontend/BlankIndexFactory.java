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
package com.janilla.blanktemplate.frontend;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.Template;
import com.janilla.frontend.cms.CmsDataFetching;
import com.janilla.frontend.cms.CmsIndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.web.ResourceMap;

public class BlankIndexFactory<C extends BlankFrontendConfig> extends CmsIndexFactory<C> {

	public BlankIndexFactory(C config, ResourceMap resourceMap, DiFactory diFactory, CmsDataFetching dataFetching) {
		super(config, resourceMap, diFactory, dataFetching);
	}

	public Template blankTemplate(String name) {
		return template(name);
	}

	@Override
	protected void putAppInitArgs(Map<String, Object> args, HttpExchange exchange) {
		super.putAppInitArgs(args, exchange);
		args.put("key", config.key());
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);
		Stream.of("app", "lucide-icon", "not-found", "page").map(this::blankImportKey)
				.forEach(x -> map.put(x, "/" + x + ".js"));
	}

	protected String blankImportKey(String name) {
		return name;
	}

	@Override
	protected void addTemplates(List<Template> list) {
		Stream.of("app", "not-found", "page").map(this::blankTemplate).forEach(list::add);
	}
}
