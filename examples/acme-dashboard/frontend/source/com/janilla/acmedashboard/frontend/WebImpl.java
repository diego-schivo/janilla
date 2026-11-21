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

import com.janilla.acmedashboard.AcmeDashboardDomain;
import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.web.Handle;

class WebImpl extends BlankWeb<AcmeDashboardDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, AcmeDashboardDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

	@Handle(method = "GET", path = "/login")
	public Index login() {
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/dashboard")
	public Index dashboard() {
		var i = indexFactory.newIndex();

		var d = apiClient.dashboard();
		var oo = new Object[3];
//		IO.println(LocalDateTime.now() + ", 1");
		for (var t : List.of(Thread.startVirtualThread(() -> oo[0] = d.cards()),
				Thread.startVirtualThread(() -> oo[1] = d.revenue()),
				Thread.startVirtualThread(() -> oo[2] = d.invoices())))
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
	public Index invoices(String query, Integer page) {
		var i = indexFactory.newIndex();

		var p = page != null ? page.intValue() : 1;
		i.app().state().put("invoices", apiClient.invoices().read(query, (p - 1) * 6L, 6L));

		return i;
	}

//	@Handle(method = "GET", path = "/dashboard/invoices/create")
//	public Object createInvoice() {
//		var i = indexFactory.newIndex();
//		i.app().state().put("invoice",
//				new Invoice2(null, apiClient(HttpExchange.SCOPED.get().request()).customerNames()));
//		return i;
//	}
//
//	@Handle(method = "GET", path = "/dashboard/invoices/([^/]+)/edit")
//	public Object editInvoice(UUID id) {
//		var i = indexFactory.newIndex();
//		var f = apiClient(HttpExchange.SCOPED.get().request());
//		i.app().state().put("invoice", new Invoice2(f.invoice(id), f.customerNames()));
//		return i;
//	}

	@Handle(method = "GET", path = "/dashboard/customers")
	public Index customers(String query) {
		var i = indexFactory.newIndex();
		i.app().state().put("customers", apiClient.customers().read(query));

		return i;
	}

}
