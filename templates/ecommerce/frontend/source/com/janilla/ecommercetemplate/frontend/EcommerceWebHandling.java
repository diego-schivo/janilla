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
package com.janilla.ecommercetemplate.frontend;

import com.janilla.java.Configuration;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontendHttpExchange;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.frontend.Script;
import com.janilla.http.HttpExchange;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.websitetemplate.WebsiteDomain;
import com.janilla.websitetemplate.frontend.WebsiteDataFetching;
import com.janilla.websitetemplate.frontend.WebsiteWebHandling;

public class EcommerceWebHandling extends WebsiteWebHandling {

	protected final Configuration configuration;

	protected final String configurationKey;

	public EcommerceWebHandling(IndexFactory indexFactory, WebsiteDomain domain, WebsiteDataFetching dataFetching,
			Configuration configuration, String configurationKey) {
		super(indexFactory, domain, dataFetching);
		this.configuration = configuration;
		this.configurationKey = configurationKey;
	}

	@Handle(method = "GET", path = "/account")
	public Index account(HttpExchange exchange) {
//		IO.println("WebHandling.account");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/account/addresses")
	public Index addresses(HttpExchange exchange) {
//		IO.println("WebHandling.addresses");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/checkout")
	public Index checkout(HttpExchange exchange) {
//		IO.println("WebHandling.checkout");
		var i = indexFactory.newIndex(exchange);
		i.scripts().add(new Script(configuration.getProperty(configurationKey + ".stripe.url")));
		return i;
	}

	@Handle(method = "GET", path = "/checkout/confirm-order")
	public Index confirmOrder(HttpExchange exchange) {
//		IO.println("WebHandling.confirmOrder");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/create-account")
	public Index createAccount(HttpExchange exchange) {
//		IO.println("WebHandling.createAccount");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/find-order")
	public Index findOrder(HttpExchange exchange) {
//		IO.println("WebHandling.findOrder");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/login")
	public Index login(HttpExchange exchange) {
//		IO.println("WebHandling.login");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/logout")
	public Index logout(HttpExchange exchange) {
//		IO.println("WebHandling.logout");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/orders/(\\d+)")
	public Index order(Long id, String guestEmail, HttpExchange exchange) {
//		IO.println("WebHandling.order, id=" + id + ", email=" + email);
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/orders")
	public Index orders(HttpExchange exchange) {
//		IO.println("WebHandling.orders");
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "GET", path = "/products/([\\w\\d-]+)")
	public Index product(String slug, HttpExchange exchange) {
//		IO.println("WebHandling.product, slug=" + slug);
		var pp = ((EcommerceDataFetching) dataFetching).products(slug, null, null, null, 3,
				((BlankFrontendHttpExchange) exchange).tokenCookie());
		if (pp.totalSize() == 0)
			throw new NotFoundException("slug=" + slug);
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("product", pp.elements().getFirst());
		Stream.of("call-to-action", "content", "media-block", "price", "product", "product-description",
				"product-gallery", "variant-selector").map(((EcommerceIndexFactory) indexFactory)::ecommerceTemplate)
				.forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/shop")
	public Index shop(@Bind("q") String query, Long category, String sort, HttpExchange exchange) {
//		IO.println("WebHandling.shop, query=" + query + ", category=" + category);
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("categories", ((EcommerceDataFetching) dataFetching).categories().elements());
		i.app().state().put("products",
				((EcommerceDataFetching) dataFetching)
						.products(null, query, category, sort, 1, ((BlankFrontendHttpExchange) exchange).tokenCookie())
						.elements());
		Stream.of("card", "shop").map(((EcommerceIndexFactory) indexFactory)::ecommerceTemplate)
				.forEach(i.templates()::add);
		return i;
	}
}
