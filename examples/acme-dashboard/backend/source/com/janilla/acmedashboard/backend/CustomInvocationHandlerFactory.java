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
package com.janilla.acmedashboard.backend;

import java.util.Set;

import com.janilla.backend.web.BackendConfig;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.HandleException;
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
		var ex = (HttpExchangeImpl) exchange;
		var rq = ex.request();
		var rs = ex.response();

		if (config.liveDemo() != null && config.liveDemo())
			if (rq.getHeaderValue(":method").equals("GET") || rq.getPath().equals("/api/authentication"))
				;
			else
				throw new HandleException(new MethodBlockedException());

		if (rq.getPath().startsWith("/api/")) {
			if (rq.getHeaderValue(":method").equals("OPTIONS") || rq.getPath().equals("/api/authentication"))
				;
			else
				ex.requireSessionEmail();
		} else if (Set.of("/", "/login").contains(rq.getPath()) || rq.getPath().contains("."))
			;
		else if (ex.getSessionEmail() == null) {
			rs.setHeaderValue(":status", "302");
			rs.setHeaderValue("cache-control", "no-cache");
			rs.setHeaderValue("location", "/login");
		}

		var o = config.api().cors().origin();
		if (o != null && !o.isEmpty()) {
			rs.setHeaderValue("access-control-allow-credentials", "true");
			rs.setHeaderValue("access-control-allow-origin", o);
		}

//		if (rq.getPath().startsWith("/api/")) {
		//// IO.println(LocalDateTime.now() + ", " + rq.getPath() + ", 1");
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		//// IO.println(LocalDateTime.now() + ", " + rq.getPath() + ", 2");
//		}

		return super.handle(invocation, exchange);
	}
}
