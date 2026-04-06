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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.janilla.cms.User;
import com.janilla.websitetemplate.WebsiteDomain;

public class EcommerceDomain extends WebsiteDomain {

	public Stream<Country> countries() {
		return Arrays.stream(CountryImpl.class.getEnumConstants());
	}

	public Currency currency(String name) {
		return CurrencyImpl.valueOf(name);
	}

	public Address emptyAddress() {
		return AddressImpl.EMPTY;
	}

	public Cart emptyCart() {
		return CartImpl.EMPTY;
	}

	public Order newOrder(List<CartItem> items, AddressData shippingAddress, User<?> customer, String customerEmail,
			List<Transaction> transactions, OrderStatus status, BigDecimal amount, Currency currency) {
		return new OrderImpl(null, items, shippingAddress, customer, customerEmail, transactions, status, amount,
				currency, null, null, null, null);
	}

	public Transaction newTransaction(List<CartItem> items, PaymentMethod paymentMethod, AddressData billingAddress,
			TransactionStatus status, User<?> customer, String customerEmail, Order order, Cart cart, BigDecimal amount,
			Currency currency, String stripeCustomer, String stripePaymentIntent) {
		return new TransactionImpl(null, items, paymentMethod, billingAddress, status, customer, customerEmail, order,
				cart, amount, currency, stripeCustomer, stripePaymentIntent, null, null, null, null);
	}

	public TransactionStatus transactionStatus(String name) {
		return TransactionStatusImpl.valueOf(name);
	}

	public OrderStatus orderStatus(String name) {
		return OrderStatusImpl.valueOf(name);
	}

	public PaymentMethod paymentMethod(String name) {
		return PaymentMethodImpl.valueOf(name);
	}

	public Stream<Title> titles() {
		return Arrays.stream(TitleImpl.class.getEnumConstants());
	}
}
