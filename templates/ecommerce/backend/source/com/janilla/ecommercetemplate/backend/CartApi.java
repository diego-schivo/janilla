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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.UnauthorizedException;

@Handle(path = "/api/carts")
public class CartApi extends AbstractCollectionApi<Long, Cart> {

	private static final Logger LOGGER = System.getLogger(CartApi.class.getName());

	public CartApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier) {
		super(Cart.class, drafts, persistence, "title", copier, Direction.FORWARD, 0);
	}

	@Handle(method = "POST")
	public Cart create(Cart entity, UserHttpExchange<User<?>> exchange) {
		var e = entity;
		var u = exchange.sessionUser();
		if (u != null)
			e = entity.withCustomer(u);
		return super.create(e);
	}

	@Handle(method = "GET", path = "(\\d+)")
	public Cart read(Long id, String secret, Integer depth) {
		LOGGER.log(Level.DEBUG, "id={0}, secret={1} depth={2}", id, secret, depth);

		@SuppressWarnings("unchecked")
		var e = (UserHttpExchange<User<?>>) HttpExchange.SCOPED.get();
		var u = e.sessionUser();
		if (u == null && (secret == null || secret.isBlank()))
			throw new UnauthorizedException();
		var c = super.read(id, depth);
		if (u == null && c != null && !c.secret().equals(secret))
			throw new ForbiddenException();
		return c;
	}
}
