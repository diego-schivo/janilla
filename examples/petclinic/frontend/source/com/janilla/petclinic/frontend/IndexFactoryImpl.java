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
package com.janilla.petclinic.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontendConfig;
import com.janilla.blanktemplate.frontend.BlankIndexFactory;
import com.janilla.frontend.ApiClient;
import com.janilla.frontend.App;
import com.janilla.frontend.DefaultApp;
import com.janilla.frontend.Template;
import com.janilla.ioc.DiFactory;
import com.janilla.web.Renderable;
import com.janilla.web.ResourceMap;

class IndexFactoryImpl extends BlankIndexFactory<BlankFrontendConfig> {

	static final ScopedValue<Renderable<?>> APP_CONTENT = ScopedValue.newInstance();

	public IndexFactoryImpl(BlankFrontendConfig config, ResourceMap resourceMap, DiFactory diFactory,
			ApiClient apiClient) {
		super(config, resourceMap, diFactory, apiClient);
	}

	@Override
	protected App newApp() {
		var aa = new HashMap<String, Object>();
		putAppInitArgs(aa);
		var a = diFactory.newInstance(
				aa.containsKey("content") ? diFactory.classFor(App.class) : (Class<? extends App>) DefaultApp.class,
				aa);
//		IO.println("DefaultIndexFactory.newApp, a=" + a);

		return a;
	}

	@Override
	protected void putAppInitArgs(Map<String, Object> args) {
		super.putAppInitArgs(args);

		if (APP_CONTENT.isBound())
			args.put("content", APP_CONTENT.get());
	}

	@Override
	public Template blankTemplate(String name) {
		return template("blank/" + name);
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);

		Stream.of("app").forEach(x -> map.put(x, config.basePath() + "/" + x + ".js"));
	}

	@Override
	protected String blankImportKey(String name) {
		return "blank/" + name;
	}

	@Override
	protected void addTemplates(List<Template> list) {
		super.addTemplates(list);

		Stream.of("app").map(this::template).forEach(list::add);
	}
}
