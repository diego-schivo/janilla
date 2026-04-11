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
package com.janilla.addressbook.frontend;

import java.io.IOException;

import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;

class WebHandling {

	protected final DataFetching dataFetching;

	protected final IndexFactory indexFactory;

	public WebHandling(DataFetching dataFetching, IndexFactory indexFactory) {
		this.dataFetching = dataFetching;
		this.indexFactory = indexFactory;
	}

	@Handle(method = "GET", path = "/")
	public Index root(String q, HttpExchange exchange) throws IOException {
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("contacts", dataFetching.contacts(q));
		return i;
	}

	@Handle(method = "GET", path = "/contacts/([^/]+)(/edit)?")
	public Index contact(String id, String edit, String q, HttpExchange exchange) {
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("contacts", dataFetching.contacts(q));
		i.app().state().put("contact", dataFetching.contact(id));
		return i;
	}

	@Handle(method = "GET", path = "/about")
	public Index about(HttpExchange exchange) {
		return indexFactory.newIndex(exchange);
	}
}
