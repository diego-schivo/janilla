/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
package com.janilla.blanktemplate.backend;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Converter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.NullTypeResolver;
import com.janilla.java.TypeResolver;
import com.janilla.java.UriQueryBuilder;
import com.janilla.web.HandleException;
import com.janilla.web.Invocation;
import com.janilla.web.InvocationHandlerFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;

public class BlankBackendInvocationHandlerFactory extends InvocationHandlerFactory {

	protected final Configuration configuration;

	protected final String configurationKey;

	protected final Set<String> guestPost;

	protected final Set<String> userLoginLogout;

	public BlankBackendInvocationHandlerFactory(InvocationResolver invocationResolver,
			RenderableFactory renderableFactory, HttpHandlerFactory rootFactory, DiFactory diFactory,
			Configuration configuration, String configurationKey) {
		super(invocationResolver, renderableFactory, rootFactory, diFactory);
		this.configuration = configuration;
		this.configurationKey = configurationKey;
		guestPost = Stream.of("/api/users/first-register", "/api/users/forgot-password", "/api/users/login",
				"/api/users/reset-password").collect(Collectors.toCollection(HashSet::new));
		userLoginLogout = Stream.of("/api/users/login", "/api/users/logout")
				.collect(Collectors.toCollection(HashSet::new));
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.request();
		if (requireSessionEmail(rq))
			((UserHttpExchange<?>) exchange).requireSessionEmail();

		if (Boolean.parseBoolean(configuration.getProperty(configurationKey + ".live-demo"))) {
			if (rq.getMethod().equals("GET") || userLoginLogout.contains(rq.getPath()))
				;
			else
				throw new HandleException(new MethodBlockedException());
		}

		var o = configuration.getProperty(configurationKey + ".api.cors.origin");
		if (o != null && !o.isEmpty()) {
			var rs = exchange.response();
			rs.setHeaderValue("access-control-allow-credentials", "true");
			rs.setHeaderValue("access-control-allow-origin", o);
		}

//		if (r.getPath().startsWith("/api/"))
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		return super.handle(invocation, exchange);
	}

	protected boolean requireSessionEmail(HttpRequest request) {
		if (!request.getPath().startsWith("/api/"))
			return false;
		switch (request.getMethod()) {
		case "GET", "OPTIONS":
			if (request.getPath().equals("/api/users"))
				return !"0".equals(new UriQueryBuilder(request.getQuery()).values("limit").findFirst().orElse(null));
			return false;
		case "POST":
			return !guestPost.contains(request.getPath());
		default:
			return true;
		}
	}

	@Override
	protected Converter converter(Class<? extends TypeResolver> type) {
		return diFactory.newInstance(Converter.class,
				type != DollarTypeResolver.class
						? Collections.singletonMap("typeResolver",
								type != null && type != NullTypeResolver.class ? diFactory.newInstance(type) : null)
						: null);
	}
}
