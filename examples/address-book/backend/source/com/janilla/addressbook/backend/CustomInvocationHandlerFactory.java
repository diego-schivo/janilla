/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
 * Copyright (c) Diego Schivo 2024-2026
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
package com.janilla.addressbook.backend;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.web.HandleException;
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
		var rq = exchange.request();
		var rs = exchange.response();

		if (Boolean.parseBoolean(configuration.getProperty("address-book.live-demo"))) {
			if (!rq.getHeaderValue(":method").equals("GET"))
				throw new HandleException(new MethodBlockedException());
		}

		var o = configuration.getProperty("address-book.api.cors.origin");
		if (o != null && !o.isEmpty())
			rs.setHeaderValue("access-control-allow-origin", o);

//		if (rq.getPath().startsWith("/api/"))
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		return super.handle(invocation, exchange);
	}
}
