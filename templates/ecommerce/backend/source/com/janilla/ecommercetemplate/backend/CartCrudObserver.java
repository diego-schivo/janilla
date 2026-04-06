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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Random;

import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.persistence.Persistence;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.ecommercetemplate.Product;
import com.janilla.ecommercetemplate.Variant;

public class CartCrudObserver implements CrudObserver<Cart> {

	private static final Random RANDOM = new SecureRandom();

	protected final Persistence persistence;

	public CartCrudObserver(Persistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public Cart beforeCreate(Cart entity) {
		var c = entity;
		if (c.customer() == null && c.secret() == null) {
			var bb = new byte[20];
			RANDOM.nextBytes(bb);
			c = c.withSecret(HexFormat.of().formatHex(bb));
		}
		return computeSubtotal(c);
	}

	@Override
	public Cart beforeUpdate(Cart entity) {
		return computeSubtotal(entity);
	}

	protected Cart computeSubtotal(Cart cart) {
		return cart.withSubtotal(cart.items().stream().map(x -> {
			BigDecimal p;
			if (x.variant() != null)
				p = persistence.crud(Variant.class).read(x.variant().id()).priceInUsd();
			else if (x.product() != null)
				p = persistence.crud(Product.class).read(x.product().id()).priceInUsd();
			else
				p = null;
			return p != null ? p.multiply(new BigDecimal(x.quantity())) : null;
		}).filter(Objects::nonNull).reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO));
	}
}
