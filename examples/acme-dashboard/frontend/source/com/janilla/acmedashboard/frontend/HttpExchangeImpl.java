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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.janilla.http.HttpCookie;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.SimpleHttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.json.Jwt;
import com.janilla.web.UnauthorizedException;

class HttpExchangeImpl extends SimpleHttpExchange {

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final Map<String, Object> session = new HashMap<>();

	public HttpExchangeImpl(HttpRequest request, HttpResponse response, Configuration configuration,
			DiFactory diFactory) {
		super(request, response);
		this.configuration = configuration;
		this.diFactory = diFactory;
//		IO.println("HttpExchangeImpl, configuration=" + configuration);
	}

	public String getSessionEmail() {
		if (!session.containsKey("email")) {
			var c = request().getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; ")))
					.map(HttpCookie::parse).filter(x -> x.name().equals("session")).findFirst().orElse(null);
//			IO.println("HttpExchangeImpl.getSessionEmail, c=" + c);
			Map<String, ?> p;
			try {
				p = c != null && c.value() != null
						? Jwt.verifyToken(c.value(), configuration.getProperty("acme-dashboard.jwt.key"))
						: null;
			} catch (IllegalArgumentException e) {
				p = null;
			}
//			IO.println("HttpExchangeImpl.getSessionEmail, p=" + p);
			session.put("email", p != null ? p.get("loggedInAs") : null);
		}
		return (String) session.get("email");
	}

	public Object getSessionUser() {
		if (!session.containsKey("user")) {
			var u = diFactory.newInstance(diFactory.classFor(Fetcher.class), Map.of("request", request))
					.authentication();
			session.put("user", u);
		}
		return session.get("user");
	}

	public void requireSessionEmail() {
		var x = getSessionEmail();
		if (x == null)
			throw new UnauthorizedException();
	}
}
