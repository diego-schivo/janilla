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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.web.Handle;

class WebHandling {

	protected final IndexFactory indexFactory;

	protected final DiFactory diFactory;

	public WebHandling(IndexFactory indexFactory, DiFactory diFactory) {
		this.indexFactory = indexFactory;
		this.diFactory = diFactory;
	}

	@Handle(method = "GET", path = "/")
	public Object root(HttpExchangeImpl exchange) {
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/login")
	public Object login(HttpExchangeImpl exchange) {
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/dashboard")
	public Object dashboard(HttpExchangeImpl exchange) {
		var i = indexFactory.newIndex(exchange);
		var f = fetcher(exchange.request());
		var oo = new Object[3];
//		IO.println(LocalDateTime.now() + ", 1");
		for (var t : List.of(Thread.startVirtualThread(() -> oo[0] = f.dashboardCards()),
				Thread.startVirtualThread(() -> oo[1] = f.dashboardRevenue()),
				Thread.startVirtualThread(() -> oo[2] = f.dashboardInvoices())))
			try {
				t.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
//		IO.println(LocalDateTime.now() + ", 2");
		i.app().state().put("cards", oo[0]);
		i.app().state().put("revenue", oo[1]);
		i.app().state().put("invoices", oo[2]);
		return i;
	}

	@Handle(method = "GET", path = "/dashboard/invoices")
	public Object invoices(String query, Integer page, HttpExchangeImpl exchange) {
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("invoices", fetcher(exchange.request()).invoices(query, page));
		return i;
	}

	@Handle(method = "GET", path = "/dashboard/invoices/create")
	public Object createInvoice(HttpExchangeImpl exchange) {
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("invoice", new Invoice2(null, fetcher(exchange.request()).customerNames()));
		return i;
	}

	@Handle(method = "GET", path = "/dashboard/invoices/([^/]+)/edit")
	public Object editInvoice(UUID id, HttpExchangeImpl exchange) {
		var i = indexFactory.newIndex(exchange);
		var f = fetcher(exchange.request());
		i.app().state().put("invoice", new Invoice2(f.invoice(id), f.customerNames()));
		return i;
	}

	@Handle(method = "GET", path = "/dashboard/customers")
	public Object customers(String query, HttpExchangeImpl exchange) {
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("customers", fetcher(exchange.request()).customers(query));
		return i;
	}

	protected Fetcher fetcher(HttpRequest request) {
		return diFactory.newInstance(diFactory.classFor(Fetcher.class), Map.of("request", request));
	}
}
