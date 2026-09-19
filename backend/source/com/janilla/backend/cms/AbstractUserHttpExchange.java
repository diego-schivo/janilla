/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.backend.cms;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.web.BackendConfig;
import com.janilla.cms.CmsDomain;
import com.janilla.cms.User;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.DefaultHttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Java;
import com.janilla.json.Jwt;
import com.janilla.web.UnauthorizedException;

public abstract class AbstractUserHttpExchange<U extends User<?>> extends DefaultHttpExchange
		implements UserHttpExchange<U> {

	protected final BackendConfig config;

	protected final Copier copier;

	protected final Crud<?, U> crud;

	protected final CmsDomain domain;

	protected final Map<String, Object> session = new HashMap<>();

	protected AbstractUserHttpExchange(HttpRequest request, HttpResponse response, BackendConfig config,
			Crud<?, U> crud, CmsDomain domain, Copier copier) {
		super(request, response);
		this.config = config;
		this.crud = crud;
		this.domain = domain;
		this.copier = copier;
	}

	public String sessionEmail() {
		if (!session.containsKey("sessionEmail")) {
			var a = request().getHeaderValue("authorization");
			var t = a != null && a.startsWith("Token ") ? a.substring("Token ".length())
					: request().getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; ")))
							.map(HttpCookie::parse).filter(x -> x.name().equals(config.jwt().cookie())).findFirst()
							.map(HttpCookie::value).orElse(null);
			Map<String, ?> p;
			try {
				p = t != null ? Jwt.verifyToken(t, config.jwt().key()) : null;
			} catch (IllegalArgumentException e) {
				p = null;
			}
			session.put("sessionEmail", p != null ? p.get("loggedInAs") : null);
		}
		return (String) session.get("sessionEmail");
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public U sessionUser() {
		if (!session.containsKey("sessionUser")) {
			var e = sessionEmail();
			var o = e != null ? ((Crud) crud).read(crud.find("email", new Object[] { e }), domain.userDepth()) : null;
			session.put("sessionUser", o);
		}
		var u = (U) session.get("sessionUser");
		return u;
	}

	@Override
	public void setSessionCookie(String value) {
		var e = value != null && !value.isBlank() ? ZonedDateTime.now(ZoneOffset.UTC).plusHours(2)
				: ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
		var m = Java.hashMap("path", "/", "httpOnly", true, "sameSite", "Lax", "expires", e);
		var c = HttpCookie.of(config.jwt().cookie(), value);
		c = copier.copy(m, c);

		response().setHeaderValue("set-cookie", c.format());
	}

	@Override
	public void requireSessionEmail() {
		if (sessionEmail() == null) {
//			var r = HttpExchange.SCOPED.get().request();
//			IO.println(r.getHeaderValue(":method") + " " + r.getPath());

			throw new UnauthorizedException("Unauthorized, you must be logged in to make this request.");
		}
	}
}
