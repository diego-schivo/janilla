/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
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
package com.janilla.addressbook.frontend;

import java.lang.reflect.Type;
import java.net.URI;

import com.janilla.addressbook.Contact;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.DefaultHttpRequest;
import com.janilla.http.HttpClient;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

class ContactApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	public ContactApiClient(FrontendConfig config, HttpClient httpClient, Converter converter) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
	}

	public ListPortion<Contact> read(String search) {
		var u = URI.create(config.api().url() + "/contacts?" + new UriQueryBuilder().append("search", search));
		var o = httpClient.send(new DefaultHttpRequest("GET", u), HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { Contact.class }));
	}

	public Contact read1(String id) {
		var u = URI.create(config.api().url() + "/contacts/" + id);
		var o = httpClient.send(new DefaultHttpRequest("GET", u), HttpClient.JSON);
		return converter.convert(o, Contact.class);
	}

}
