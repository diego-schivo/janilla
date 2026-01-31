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
package com.janilla.backend.cms;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.janilla.backend.persistence.Crud;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.SimpleHttpExchange;
import com.janilla.json.Jwt;
import com.janilla.web.UnauthorizedException;

public abstract class AbstractUserHttpExchange<U extends User<?, ?>> extends SimpleHttpExchange
		implements UserHttpExchange<U> {

	protected final String jwtCookie;

	protected final String jwtKey;

	protected final Crud<?, U> userCrud;

	protected final Map<String, Object> session = new HashMap<>();

	protected AbstractUserHttpExchange(HttpRequest request, HttpResponse response, String jwtCookie, String jwtKey,
			Crud<?, U> userCrud) {
		super(request, response);
		this.jwtCookie = jwtCookie;
		this.jwtKey = jwtKey;
		this.userCrud = userCrud;
	}

	public String sessionEmail() {
		if (!session.containsKey("sessionEmail")) {
			var t = request().getHeaderValues("cookie").map(HttpCookie::parse).filter(x -> x.name().equals(jwtCookie))
					.findFirst().orElse(null);
			Map<String, ?> p;
			try {
				p = t != null ? Jwt.verifyToken(t.value(), jwtKey) : null;
			} catch (IllegalArgumentException e) {
				p = null;
			}
			session.put("sessionEmail", p != null ? p.get("loggedInAs") : null);
		}
		return (String) session.get("sessionEmail");
	}

	@Override
	public U sessionUser() {
		if (!session.containsKey("sessionUser")) {
			var e = sessionEmail();
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var o = e != null ? ((Crud) userCrud).read(userCrud.find("email", e)) : null;
			session.put("sessionUser", o);
		}
		@SuppressWarnings("unchecked")
		var u = (U) session.get("sessionUser");
		return u;
	}

	@Override
	public void setSessionCookie(String value) {
		response().setHeaderValue("set-cookie",
				HttpCookie.of(jwtCookie, value).withPath("/").withHttpOnly(true).withSameSite("Lax")
						.withExpires(value != null && !value.isEmpty() ? ZonedDateTime.now(ZoneOffset.UTC).plusHours(2)
								: ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
						.format());
	}

	@Override
	public void requireSessionEmail() {
		if (sessionEmail() == null)
			throw new UnauthorizedException("Unauthorized, you must be logged in to make this request.");
	}
}
