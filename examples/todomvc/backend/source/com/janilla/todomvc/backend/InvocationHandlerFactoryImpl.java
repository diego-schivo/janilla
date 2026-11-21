/*
 * MIT License
 *
 * Copyright (c) Addy Osmani, Sindre Sorhus, Pascal Hartig, Stephen Sawchuk.
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.janilla.todomvc.backend;

import com.janilla.blanktemplate.backend.BlankBackendInvocationHandlerFactory;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

class InvocationHandlerFactoryImpl extends BlankBackendInvocationHandlerFactory {

	public InvocationHandlerFactoryImpl(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			InvocationResolver invocationResolver, RenderableFactory renderableFactory) {
		super(config, rootFactory, diFactory, invocationResolver, renderableFactory);

		guestPost.add("/api/todo-items");
	}

	@Override
	protected boolean requireSessionEmail(HttpRequest request) {
		if (!super.requireSessionEmail(request))
			return false;

		switch (request.getHeaderValue(":method")) {
		case "DELETE", "PATCH":
			var p = webAppPath(request);
			return !(p.equals("/api/todo-items") || p.startsWith("/api/todo-items/"));
		default:
			return true;
		}
	}
}
