/*
 * Copyright 2012-2025 the original author or authors.
 * Copyright 2024-2026 Diego Schivo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @author Diego Schivo
 */
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

		Stream.of("admin", "app").forEach(x -> map.put(x, config.basePath() + "/" + x + ".js"));
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
