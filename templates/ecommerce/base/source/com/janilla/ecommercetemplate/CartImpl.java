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
package com.janilla.ecommercetemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.janilla.cms.DocumentStatus;
import com.janilla.cms.User;

record CartImpl(Long id, List<CartItem> items, String secret, User<?> customer, Instant purchasedAt, CartStatus status,
		BigDecimal subtotal, Currency currency, Instant createdAt, Instant updatedAt, DocumentStatus documentStatus,
		Instant publishedAt) implements Cart {

	public static final CartImpl EMPTY = new CartImpl(null, null, null, null, null, null, null, null, null, null, null,
			null);

	@Override
	public Cart withId(Long id) {
		return new CartImpl(id, items, secret, customer, purchasedAt, status, subtotal, currency, createdAt, updatedAt,
				documentStatus, publishedAt);
	}

	@Override
	public Cart withCustomer(User<?> customer) {
		return new CartImpl(id, items, secret, customer, purchasedAt, status, subtotal, currency, createdAt, updatedAt,
				documentStatus, publishedAt);
	}

	@Override
	public Cart withPurchasedAt(Instant purchasedAt) {
		return new CartImpl(id, items, secret, customer, purchasedAt, status, subtotal, currency, createdAt, updatedAt,
				documentStatus, publishedAt);
	}

	@Override
	public Cart withSecret(String secret) {
		return new CartImpl(id, items, secret, customer, purchasedAt, status, subtotal, currency, createdAt, updatedAt,
				documentStatus, publishedAt);
	}

	@Override
	public Cart withSubtotal(BigDecimal subtotal) {
		return new CartImpl(id, items, secret, customer, purchasedAt, status, subtotal, currency, createdAt, updatedAt,
				documentStatus, publishedAt);
	}
}
