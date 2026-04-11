/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
package com.janilla.acmedashboard.frontend;

import java.util.Objects;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.web.Invocation;
import com.janilla.web.InvocationHandlerFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;

class CustomInvocationHandlerFactory extends InvocationHandlerFactory {

	protected final Configuration configuration;

	public CustomInvocationHandlerFactory(InvocationResolver invocationResolver, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory, DiFactory diFactory, Configuration configuration) {
		super(invocationResolver, renderableFactory, rootFactory, diFactory);
		this.configuration = configuration;
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		var ex = (HttpExchangeImpl) exchange;
		var rq = ex.request();
		var rs = ex.response();

		if ((rq.getPath() + "/").startsWith("/dashboard/"))
			ex.requireSessionEmail();
		else if (!Objects.requireNonNullElse(ex.getSessionEmail(), "").isEmpty()) {
			rs.setStatus(303);
			rs.setHeaderValue("cache-control", "no-cache");
			rs.setHeaderValue("location", "/dashboard");
			return true;
		}

		return super.handle(invocation, exchange);
	}
}
