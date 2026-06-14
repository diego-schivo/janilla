/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.frontend;

import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.DefaultTemplateHandlerFactory;
import com.janilla.web.Renderable;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

/**
 * @author Diego Schivo
 */
class TemplateHandlerFactoryImpl extends DefaultTemplateHandlerFactory {

	protected final IndexFactory indexFactory;

	protected final RenderableFactory renderableFactory;

	public TemplateHandlerFactoryImpl(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			RenderableFactory renderableFactory, IndexFactory indexFactory) {
		super(config, rootFactory, diFactory);
		this.renderableFactory = renderableFactory;
		this.indexFactory = indexFactory;
	}

	@Override
	protected void render(Renderable<?> input, HttpExchange exchange) {
		if (input.value() instanceof Index)
			super.render(input, exchange);
		else
			ScopedValue.where(IndexFactoryImpl.APP_CONTENT, input).run(() -> {
				var i = indexFactory.newIndex();
				var r = renderableFactory.createRenderable(null, i);
				super.render(r, exchange);
			});
	}
}
