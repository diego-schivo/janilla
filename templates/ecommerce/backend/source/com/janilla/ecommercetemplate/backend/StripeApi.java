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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.AddressData;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.ecommercetemplate.CartItem;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.ecommercetemplate.Order;
import com.janilla.ecommercetemplate.Transaction;
import com.janilla.http.DefaultHttpClient;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Configuration;
import com.janilla.java.DefaultConverter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.json.Json;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;

@Handle(path = "/api/payments/stripe")
public class StripeApi extends PaymentApi {

	protected final EcommerceDomain domain;

	protected final String secretKey = configuration.getProperty("ecommerce-template.stripe.secret-key");

	public StripeApi(Configuration configuration, Persistence persistence, EcommerceDomain domain) {
		super(configuration, persistence);
		this.domain = domain;
	}

	@Override
	protected InitiateResult initiate(User<?> user, String guestEmail, Cart cart, AddressData billingAddress,
			AddressData shippingAddress) {
		record C(String id) {
		}
		record R(List<C> data) {
		}
		C c;
		{
			var rq = new HttpRequest("GET", URI.create("https://api.stripe.com/v1/customers?"
					+ new UriQueryBuilder().append("email", user != null ? user.email() : guestEmail)));
			rq.setBasicAuthorization(secretKey + ":");
			var r = new DefaultHttpClient().send(rq,
					HttpClient.JSON.andThen(x -> (R) new DefaultConverter().convert(x, R.class)));
//			IO.println("r=" + r);
			c = !r.data().isEmpty() ? r.data().getFirst() : null;
		}

		if (c == null) {
			var rq = new HttpRequest("POST", URI.create("https://api.stripe.com/v1/customers"));
			rq.setBasicAuthorization(secretKey + ":");
			var bb = new UriQueryBuilder().append("email", user != null ? user.email() : guestEmail).toString()
					.getBytes();
			rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
			rq.setHeaderValue("content-length", String.valueOf(bb.length));
			rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
			c = new DefaultHttpClient().send(rq,
					HttpClient.JSON.andThen(x -> new DefaultConverter().convert(x, C.class)));
//			IO.println("c=" + c);
		}

		record PI(String id, String client_secret) {
		}
		PI pi;
		{
			var rq = new HttpRequest("POST", URI.create("https://api.stripe.com/v1/payment_intents"));
			rq.setBasicAuthorization(secretKey + ":");
			var q = new UriQueryBuilder()
					.append("amount", String.valueOf(cart.subtotal().multiply(BigDecimal.valueOf(100)).longValue()))
					.append("automatic_payment_methods[enabled]", "true")
					.append("currency", cart.currency().toString().toLowerCase()).append("customer", c.id())
					.append("metadata[cartId]", cart.id().toString())
					.append("metadata[cartItems]", Json.format(cart.items(), true))
					.append("metadata[shippingAddress]", Json.format(shippingAddress, true)).toString();
//			IO.println("q=" + q);
			var bb = q.getBytes();
			rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
			rq.setHeaderValue("content-length", String.valueOf(bb.length));
			rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
			pi = new DefaultHttpClient().send(rq,
					HttpClient.JSON.andThen(x -> new DefaultConverter().convert(x, PI.class)));
//			IO.println("pi=" + pi);
		}

		persistence.crud(Transaction.class)
				.create(domain.newTransaction(cart.items(), domain.paymentMethod("STRIPE"), billingAddress,
						domain.transactionStatus("PENDING"), user, guestEmail, null, cart, cart.subtotal(),
						cart.currency(), c.id(), pi.id()));

		return new InitiateResult(pi.id(), pi.client_secret());
	}

	@Override
	protected ConfirmOrderResult confirmOrder(User<?> user, String guestEmail, String paymentIntent) {
		Transaction t;
		{
			var x = persistence.crud(Transaction.class);
			t = x.read(x.find("stripePaymentIntent", new Object[] { paymentIntent }));
		}

		record PI(Long amount, String currency, Map<String, String> metadata) {
		}
		PI pi;
		{
			var rq = new HttpRequest("GET", URI.create("https://api.stripe.com/v1/payment_intents/" + paymentIntent));
			rq.setBasicAuthorization(configuration.getProperty("ecommerce-template.stripe.secret-key") + ":");
			pi = new DefaultHttpClient().send(rq,
					HttpClient.JSON.andThen(x -> new DefaultConverter().convert(x, PI.class)));
//			IO.println("pi=" + pi);
		}

		@SuppressWarnings("unchecked")
		var cii = (List<CartItem>) new DefaultConverter().convert(Json.parse(pi.metadata().get("cartItems")),
				new SimpleParameterizedType(List.class, List.of(CartItem.class)));
		var sa = (AddressData) new DefaultConverter().convert(Json.parse(pi.metadata().get("shippingAddress")),
				AddressData.class);
		var o = persistence.crud(Order.class)
				.create(domain.newOrder(cii, sa, user, guestEmail, List.of(t), domain.orderStatus("PROCESSING"),
						BigDecimal.valueOf(pi.amount(), 2), domain.currency(pi.currency().toUpperCase())));

		persistence.crud(Cart.class).update(Long.valueOf(pi.metadata().get("cartId")),
				x -> x.withPurchasedAt(Instant.now()));

		persistence.crud(Transaction.class).update(t.id(),
				x -> x.withOrder(o).withStatus(domain.transactionStatus("SUCCEEDED")));

		return new ConfirmOrderResult(o.id(), t.id());
	}

	@Handle(method = "POST", path = "webhooks")
	public void webhooks(HttpRequest request) throws IOException {
		var b = request.getBody();
		if (b != null) {
			var bs = new String(Channels.newInputStream((ReadableByteChannel) b).readAllBytes());
//			IO.println("StripeApi.webhooks, bs=" + bs);

			try {
				var ss = request.getHeaderValue("stripe-signature");
//				IO.println("StripeApi.webhooks, ss=" + ss);
				var ssm = Arrays.stream(ss.split(",")).map(x -> x.split("=", 2))
						.collect(Collectors.toMap(x -> x[0], x -> x[1]));
//				IO.println("StripeApi.webhooks, ssm=" + ssm);

				var k = configuration.getProperty("ecommerce-template.stripe.webhooks-signing-secret");
				var m = ssm.get("t") + "." + bs;

				var a = Mac.getInstance("HmacSHA256");
				a.init(new SecretKeySpec(k.getBytes(), "HmacSHA256"));
				var r = HexFormat.of().formatHex(a.doFinal(m.getBytes()));
//				IO.println("StripeApi.webhooks, r=" + r);

				if (!r.equals(ssm.get("v1")))
					throw new ForbiddenException();
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}

//			var j = Json.parse(bs);
//			@SuppressWarnings("unchecked")
//			var m = j instanceof Map x ? (Map<String, Object>) x : null;
//			var t = m != null && m.get("type") instanceof String x ? x : null;
//			if (t != null && t.equals("payment_intent.succeeded")) {
//				@SuppressWarnings("unchecked")
//				var d = m.get("data") instanceof Map x ? (Map<String, Object>) x : null;
//				@SuppressWarnings("unchecked")
//				var o = d != null && d.get("object") instanceof Map x ? (Map<String, Object>) x : null;
//				IO.println("o=" + o);
//				var uc = persistence.crud(User.class);
//				var u = uc.read(uc.filter("stripeCustomerId", (String) o.get("customer")).getFirst());
//				var o2 = persistence.crud(Order.class).create(new Order(null, u.id(), (String) o.get("id"),
//						(Long) o.get("amount"), null, null, OrderStatus.PROCESSING, null, null, null, null));
//				var q = ORDERS.computeIfAbsent(o2.stripePaymentIntentId(), _ -> new ArrayBlockingQueue<>(1));
//				try {
//					q.put(o2);
//				} catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//			}
		}
	}
}
