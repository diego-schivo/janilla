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
package com.janilla.petclinic.backend;

import com.janilla.blanktemplate.backend.BlankBackendInvocationHandlerFactory;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

/**
 * @author Diego Schivo
 */
class InvocationHandlerFactoryImpl extends BlankBackendInvocationHandlerFactory {

	public InvocationHandlerFactoryImpl(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			InvocationResolver invocationResolver, RenderableFactory renderableFactory) {
		super(config, rootFactory, diFactory, invocationResolver, renderableFactory);

		guestPost.add("/api/owners");
		guestPost.add("/api/pets");
		guestPost.add("/api/visits");
	}
}
