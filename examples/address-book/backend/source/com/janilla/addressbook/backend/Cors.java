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

import java.util.Properties;
import java.util.stream.Collectors;

import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.web.Handle;
import com.janilla.web.InvocationResolver;

public class Cors {

	protected final Properties configuration;

	protected final InvocationResolver invocationResolver;

	public Cors(Properties configuration, InvocationResolver invocationResolver) {
		this.configuration = configuration;
		this.invocationResolver = invocationResolver;
	}

	@Handle(method = "OPTIONS", path = "/api/(.*)")
	public void allow(HttpRequest request, HttpResponse response) {
		var o = configuration.getProperty("address-book.api.cors.origin");
		var m = invocationResolver.groups(request.getPath()).flatMap(x -> x.methods().keySet().stream()).toList();
		var h = configuration.getProperty("address-book.api.cors.headers");

		response.setStatus(204);
		response.setHeaderValue("access-control-allow-origin", o);
		response.setHeaderValue("access-control-allow-methods",
				m.contains(null) ? "*" : m.stream().collect(Collectors.joining(", ")));
		response.setHeaderValue("access-control-allow-headers", h);
	}
}
