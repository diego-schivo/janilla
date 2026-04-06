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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.SimpleHttpExchange;
import com.janilla.json.Jwt;

public class BackendExchange extends SimpleHttpExchange {

	protected final Properties configuration;

	protected final Persistence persistence;

	protected final Map<String, Object> session = new HashMap<>();

	public BackendExchange(HttpRequest request, HttpResponse response, Properties configuration,
			Persistence persistence) {
		super(request, response);
		this.configuration = configuration;
		this.persistence = persistence;
	}

	public User getUser() {
		if (!session.containsKey("user")) {
			var a = request().getHeaderValue("authorization");
			var t = a != null && a.startsWith("Token ") ? a.substring("Token ".length()) : null;
			var p = t != null ? Jwt.verifyToken(t, configuration.getProperty("conduit.jwt.key")) : null;
			var e = p != null ? (String) p.get("loggedInAs") : null;
			User u;
			if (e != null) {
				var c = persistence.crud(User.class);
				u = c.read(c.find("email", new Object[] { e }));
			} else
				u = null;
			session.put("user", u);
		}
		return (User) session.get("user");
	}
}
