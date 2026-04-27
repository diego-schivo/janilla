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
package com.janilla.conduit.backend;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import com.janilla.backend.web.BackendConfig;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.web.Invocation;
import com.janilla.web.InvocationHandlerFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;

class CustomInvocationHandlerFactory extends InvocationHandlerFactory {

	protected final BackendConfig config;

	public CustomInvocationHandlerFactory(InvocationResolver invocationResolver, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory, DiFactory diFactory, BackendConfig config) {
		super(invocationResolver, renderableFactory, rootFactory, diFactory);
		this.config = config;
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		var o = config.api().cors().origin();
		if (o != null && !o.isEmpty())
			exchange.response().setHeaderValue("access-control-allow-origin", o);

//		if (exchange.request().getPath().startsWith("/api/"))
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		return super.handle(invocation, exchange);
	}

	@Override
	protected Object resolveArgument(Type type, HttpExchange exchange, String[] values, Supplier<String> body,
			Supplier<Converter> converter) {
		return type == User.class ? ((HttpExchangeImpl) exchange).getUser()
				: super.resolveArgument(type, exchange, values, body, converter);
	}
}
