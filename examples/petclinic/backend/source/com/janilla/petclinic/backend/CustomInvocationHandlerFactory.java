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
package com.janilla.petclinic.backend;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.HandleException;
import com.janilla.web.Invocation;
import com.janilla.web.InvocationHandlerFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

/**
 * @author Diego Schivo
 */
class CustomInvocationHandlerFactory extends InvocationHandlerFactory {

	protected final WebAppConfig config;

	public CustomInvocationHandlerFactory(InvocationResolver invocationResolver, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory, DiFactory diFactory, WebAppConfig config) {
		super(invocationResolver, renderableFactory, rootFactory, diFactory);
		this.config = config;
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		if (config.liveDemo() && !exchange.request().getHeaderValue(":method").equals("GET"))
			throw new HandleException(new InvocationBlockedException());

		return super.handle(invocation, exchange);
	}
}
