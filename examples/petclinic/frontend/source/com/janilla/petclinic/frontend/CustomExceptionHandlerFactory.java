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

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.web.Error;
import com.janilla.web.ExceptionHandlerFactory;
import com.janilla.web.HtmlRenderer;
import com.janilla.web.RenderableFactory;

/**
 * @author Diego Schivo
 */
public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

	protected final RenderableFactory renderableFactory;

	protected final HttpHandlerFactory rootFactory;

	public CustomExceptionHandlerFactory(RenderableFactory renderableFactory, HttpHandlerFactory rootFactory) {
		this.renderableFactory = renderableFactory;
		this.rootFactory = rootFactory;
	}

	@Override
	protected boolean handle(Error error, HttpExchange exchange) {
//		IO.println(
//				"CustomExceptionHandlerFactory.handle, " + exchange.request().getPath() + ", " + exchange.exception());
		var x = super.handle(error, exchange);
		var r = renderableFactory.createRenderable(null, exchange.exception());
		if (r.renderer() instanceof HtmlRenderer) {
			var h = rootFactory.createHandler(r);
//			IO.println("r=" + r + ", h=" + h);
			x = h.handle(exchange);
		}
		return x;
	}
}
