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

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Configuration;
import com.janilla.java.UriQueryBuilder;

class ClientFetcher implements Fetcher {

	protected final HttpClient client;

	protected final Configuration configuration;

	protected final HttpRequest request;

	public ClientFetcher(HttpClient httpClient, Configuration configuration, HttpRequest request) {
		this.client = httpClient;
		this.configuration = configuration;
		this.request = request;
	}

	@Override
	public Object authentication() {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/authentication"), cookie());
		return client.send(r, HttpClient.JSON);
	}

	@Override
	public List<?> customers(String query) {
		var r = new HttpRequest("GET", URI.create(configuration.getProperty("acme-dashboard.api.url") + "/customers?"
				+ new UriQueryBuilder().append("query", query)), cookie());
		return (List<?>) client.send(r, HttpClient.JSON);
	}

	@Override
	public List<?> customerNames() {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/customers/names"), cookie());
		return (List<?>) client.send(r, HttpClient.JSON);
	}

	@Override
	public Object dashboardCards() {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/cards"), cookie());
		return client.send(r, HttpClient.JSON);
	}

	@Override
	public List<?> dashboardInvoices() {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/invoices"), cookie());
		return (List<?>) client.send(r, HttpClient.JSON);
	}

	@Override
	public List<?> dashboardRevenue() {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/revenue"), cookie());
		return (List<?>) client.send(r, HttpClient.JSON);
	}

	@Override
	public Object invoice(UUID id) {
		var r = new HttpRequest("GET",
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/invoices/" + id), cookie());
		return client.send(r, HttpClient.JSON);
	}

	@Override
	public Object invoices(String query, Integer page) {
		var r = new HttpRequest("GET", URI.create(configuration.getProperty("acme-dashboard.api.url") + "/invoices?"
				+ new UriQueryBuilder().append("query", query).append("page", page != null ? page.toString() : null)),
				cookie());
		return client.send(r, HttpClient.JSON);
	}

	protected String cookie() {
		return request.getHeaderValues("cookie").filter(x -> x.startsWith("session=")).findFirst().orElse(null);
	}
}
