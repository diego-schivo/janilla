/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
package com.janilla.ecommercetemplate.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.ecommercetemplate.Order;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.UnauthorizedException;

@Handle(path = "/api/orders")
public class OrderApi extends AbstractCollectionApi<Long, Order> {

	protected final EcommerceDomain domain;

	public OrderApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier, EcommerceDomain domain) {
		super(Order.class, drafts, persistence, "title", copier);
		this.domain = domain;
	}

	@Handle(method = "GET")
	public List<Order> read(Long customer) {
		@SuppressWarnings("unchecked")
		var e = (UserHttpExchange<User<?>>) HttpExchange.SCOPED.get();
		var u = e.sessionUser();
		var rr = u != null ? u.roles() : null;
		if (rr == null || !(rr.contains(domain.userRole("ADMIN")) || rr.contains(domain.userRole("CUSTOMER"))))
			throw new UnauthorizedException();

		if (rr != null && rr.contains(domain.userRole("CUSTOMER"))) {
			if (customer == null)
				customer = (Long) u.id();
			else if (!customer.equals(u.id()))
				throw new ForbiddenException();
		}

		var oo = new ArrayList<>(
				crud().read(customer != null ? crud().filter("customer", new Object[] { customer }) : crud().list(),
						drafts.test(e), 0));
		Collections.reverse(oo);
		return oo;
	}

	@Override
	public Order read(Long id, Integer depth) {
		@SuppressWarnings("unchecked")
		var e = (UserHttpExchange<User<?>>) HttpExchange.SCOPED.get();
		var u = e.sessionUser();
		var rr = u != null ? u.roles() : null;
		if (rr == null || !(rr.contains(domain.userRole("ADMIN")) || rr.contains(domain.userRole("CUSTOMER"))))
			throw new UnauthorizedException();

		var o = super.read(id, depth);
		if (rr != null && rr.contains(domain.userRole("CUSTOMER")) && !u.id().equals(o.customer().id()))
			throw new ForbiddenException();
		return o;
	}
}
