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
import java.util.Properties;
import java.util.UUID;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.UriQueryBuilder;

public class ClientFetcher implements Fetcher {

	protected final HttpClient client;

	protected final Properties configuration;

	protected final HttpRequest request;

	public ClientFetcher(HttpClient httpClient, Properties configuration, HttpRequest request) {
		this.client = httpClient;
		this.configuration = configuration;
		this.request = request;
	}

	@Override
	public Object authentication() {
		return client.getJson(URI.create(configuration.getProperty("acme-dashboard.api.url") + "/authentication"),
				cookie());
	}

	@Override
	public List<?> customers(String query) {
		return (List<?>) client.getJson(URI.create(configuration.getProperty("acme-dashboard.api.url") + "/customers?"
				+ new UriQueryBuilder().append("query", query)), cookie());
	}

	@Override
	public List<?> customerNames() {
		return (List<?>) client.getJson(
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/customers/names"), cookie());
	}

	@Override
	public Object dashboardCards() {
		return client.getJson(URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/cards"),
				cookie());
	}

	@Override
	public List<?> dashboardInvoices() {
		return (List<?>) client.getJson(
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/invoices"), cookie());
	}

	@Override
	public List<?> dashboardRevenue() {
		return (List<?>) client.getJson(
				URI.create(configuration.getProperty("acme-dashboard.api.url") + "/dashboard/revenue"), cookie());
	}

	@Override
	public Object invoice(UUID id) {
		return client.getJson(URI.create(configuration.getProperty("acme-dashboard.api.url") + "/invoices/" + id),
				cookie());
	}

	@Override
	public Object invoices(String query, Integer page) {
		return client.getJson(URI.create(configuration.getProperty("acme-dashboard.api.url") + "/invoices?"
				+ new UriQueryBuilder().append("query", query).append("page", page != null ? page.toString() : null)),
				cookie());
	}

	protected String cookie() {
		return request.getHeaderValues("cookie").filter(x -> x.startsWith("session=")).findFirst().orElse(null);
	}
}
