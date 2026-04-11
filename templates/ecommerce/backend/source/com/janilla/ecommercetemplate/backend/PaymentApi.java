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

import com.janilla.java.Configuration;

import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.AddressData;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.web.Handle;

public abstract class PaymentApi {

	protected final Configuration configuration;

	protected final Persistence persistence;

	protected PaymentApi(Configuration configuration, Persistence persistence) {
		this.configuration = configuration;
		this.persistence = persistence;
	}

	public record InitiateData(String guestEmail, Long cart, AddressData billingAddress, AddressData shippingAddress) {
	}

	public record InitiateResult(String paymentIntent, String clientSecret) {
	}

	@Handle(method = "POST", path = "initiate")
	public InitiateResult initiate(InitiateData data, UserHttpExchange<User<?>> exchange) {
//		IO.println("PaymentApi.initiate, data=" + data);
		var u = exchange.sessionUser();
		return initiate(u, data.guestEmail(), persistence.crud(Cart.class).read(data.cart()), data.billingAddress(),
				data.shippingAddress());
	}

	public record ConfirmOrderData(String guestEmail, String paymentIntent) {
	}

	public record ConfirmOrderResult(Long order, Long transaction) {
	}

	@Handle(method = "POST", path = "confirm-order")
	public ConfirmOrderResult confirmOrder(ConfirmOrderData data, UserHttpExchange<User<?>> exchange) {
//		IO.println("PaymentApi.confirmOrder, data=" + data);
		var u = exchange.sessionUser();
		return confirmOrder(u, data.guestEmail(), data.paymentIntent());
	}

	protected abstract InitiateResult initiate(User<?> user, String guestEmail, Cart cart, AddressData billingAddress,
			AddressData shippingAddress);

	protected abstract ConfirmOrderResult confirmOrder(User<?> user, String guestEmail, String paymentIntent);
}
