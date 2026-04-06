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
package com.janilla.acmedashboard.backend;

import java.util.List;

import com.janilla.backend.persistence.Persistence;
import com.janilla.web.Handle;

@Handle(path = "/api/dashboard")
public class DashboardApi {

	protected final Persistence persistence;

	public DashboardApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "cards")
	public Cards getCards() {
		var c = (InvoiceCrud) persistence.crud(Invoice.class);
		return new Cards(c.getAmount(InvoiceStatus.PAID), c.getAmount(InvoiceStatus.PENDING), c.count(),
				persistence.crud(Customer.class).count());
	}

	@Handle(method = "GET", path = "revenue")
	public List<Revenue> getRevenue() {
		var c = persistence.crud(Revenue.class);
		return c.read(c.list());
	}

	@Handle(method = "GET", path = "invoices")
	public List<Invoice> getInvoices() {
		var c = persistence.crud(Invoice.class);
		return c.filter("date", new Object[0], true, 0, 5).stream().map(x -> c.read(x, 1)).toList();
	}
}
