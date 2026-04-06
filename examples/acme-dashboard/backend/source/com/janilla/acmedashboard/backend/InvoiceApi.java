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

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.janilla.backend.persistence.Persistence;
import com.janilla.java.JavaReflect;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/invoices")
public class InvoiceApi {

	protected final Persistence persistence;

	public InvoiceApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET")
	public ListPortion<Invoice> list(@Bind("query") String query, @Bind("page") Integer page) {
		var c = persistence.crud(Invoice.class);
		var ii = c.filter("date", new Object[0], true).stream().map(x -> c.read(x, 1));

		if (query != null && !query.isBlank())
			ii = ii.filter(x -> x.customer().name().toLowerCase().contains(query.toLowerCase()));

		var l = ii.toList();
		var p = page != null ? page.intValue() : 1;
		var i1 = (p - 1) * 6;
		var i2 = Math.min(i1 + 6, l.size());
		return new ListPortion<>(l.subList(i1, i2), l.size());
	}

	@Handle(method = "POST")
	public Invoice create(Invoice invoice) {
		return persistence.crud(Invoice.class).create(invoice.withDate(LocalDate.now()));
	}

	@Handle(method = "GET", path = "([^/]+)")
	public Invoice read(UUID id) {
		return persistence.crud(Invoice.class).read(id);
	}

	@Handle(method = "PUT", path = "([^/]+)")
	public Invoice update(UUID id, Invoice invoice) {
		return persistence.crud(Invoice.class).update(id,
				x -> JavaReflect.copy(invoice, x, y -> !Set.of("id", "date").contains(y)));
	}

	@Handle(method = "DELETE", path = "([^/]+)")
	public Invoice delete(UUID id) {
		return persistence.crud(Invoice.class).delete(id);
	}
}
