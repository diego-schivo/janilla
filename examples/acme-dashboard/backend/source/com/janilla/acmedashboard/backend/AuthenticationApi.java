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

import java.util.Map;

import com.janilla.backend.persistence.Persistence;
import com.janilla.java.Configuration;
import com.janilla.json.Jwt;
import com.janilla.web.Handle;

@Handle(path = "/api/authentication")
class AuthenticationApi {

	protected final Configuration configuration;

	protected final Persistence persistence;

	public AuthenticationApi(Configuration configuration, Persistence persistence) {
		this.configuration = configuration;
		this.persistence = persistence;
	}

	@Handle(method = "POST")
	public User create(User user, HttpExchangeImpl exchange) {
//		IO.println("AuthenticationApi.create, user=" + user);
		var c = persistence.crud(User.class);
		var u = c.read(c.find("email", new Object[] { user.email() }));
//		IO.println("AuthenticationApi.create, u=" + u);
		if (u == null || !u.password().equals(user.password()))
			return null;
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("acme-dashboard.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "GET")
	public User read(HttpExchangeImpl exchange) {
//		IO.println("AuthenticationApi.read");
		return exchange.getSessionUser();
	}

	@Handle(method = "DELETE")
	public void delete(User user, HttpExchangeImpl exchange) {
//		IO.println("AuthenticationApi.delete, user=" + user);
		exchange.setSessionCookie(null);
	}
}
