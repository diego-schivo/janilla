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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.java.Flat;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/customers")
class CustomerApi {

	protected final Persistence persistence;

	public CustomerApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "names")
	public List<Map.Entry<String, String>> names() {
		var c = persistence.crud(Customer.class);
		return new ArrayList<>(c.read(c.list()).stream()
				.collect(Collectors.toMap(x -> x.id().toString(), Customer::name, (v, _) -> v, LinkedHashMap::new))
				.entrySet());
	}

	@Handle(method = "GET")
	public List<Customer2> list(@Bind("query") String query) {
		var c = persistence.crud(Customer.class);
		return c.read(query == null || query.isEmpty() ? c.list()
				: c.filter("name", x -> ((String) x).toLowerCase().contains(query.toLowerCase()))).stream()
				.map(x -> Customer2.of(x)).toList();
	}

	public record Customer2(@Flat Customer customer, Long invoiceCount, BigDecimal pendingAmount,
			BigDecimal paidAmount) {

		public static Customer2 of(Customer customer) {
			var c = (InvoiceCrud) AcmeDashboardBackend.INSTANCE.get().persistence().crud(Invoice.class);
			return new Customer2(customer, c.count("customer", new Object[] { customer.id() }),
					c.getAmount(customer.id(), InvoiceStatus.PENDING), c.getAmount(customer.id(), InvoiceStatus.PAID));
		}
	}
}
