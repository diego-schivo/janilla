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

import java.util.stream.Stream;

import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.frontend.Script;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.websitetemplate.frontend.WebsiteWebHandling;

public class EcommerceWebHandling<D extends EcommerceDomain, C extends EcommerceApiClient>
		extends WebsiteWebHandling<D, C> {

	protected final EcommerceFrontendConfig config;

	public EcommerceWebHandling(IndexFactory indexFactory, D domain, C apiClient, EcommerceFrontendConfig config) {
		super(indexFactory, domain, apiClient);
		this.config = config;
	}

	@Handle(method = "GET", path = "/account")
	public Index account() {
//		IO.println("WebHandling.account");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/account/addresses")
	public Index addresses() {
//		IO.println("WebHandling.addresses");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/checkout")
	public Index checkout() {
//		IO.println("WebHandling.checkout");
		var i = indexFactory.newIndex();
		i.scripts().add(new Script(config.stripe().url()));
		return i;
	}

	@Handle(method = "GET", path = "/checkout/confirm-order")
	public Index confirmOrder() {
//		IO.println("WebHandling.confirmOrder");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/create-account")
	public Index createAccount() {
//		IO.println("WebHandling.createAccount");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/find-order")
	public Index findOrder() {
//		IO.println("WebHandling.findOrder");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/login")
	public Index login() {
//		IO.println("WebHandling.login");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/logout")
	public Index logout() {
//		IO.println("WebHandling.logout");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/orders/(\\d+)")
	public Index order(Long id, String guestEmail) {
//		IO.println("WebHandling.order, id=" + id + ", email=" + email);
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/orders")
	public Index orders() {
//		IO.println("WebHandling.orders");
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/products/([\\w\\d-]+)")
	public Index product(String slug) {
//		IO.println("WebHandling.product, slug=" + slug);
		var pp = ((EcommerceApiClient) apiClient).products().read(slug, null, null, null, 3, tokenCookie());
		if (pp.totalSize() == 0)
			throw new NotFoundException("slug=" + slug);
		var i = indexFactory.newIndex();
		i.app().state().put("product", pp.elements().getFirst());
		Stream.of("call-to-action", "content", "media-block", "price", "product", "product-description",
				"product-gallery", "variant-selector").map(((EcommerceIndexFactory<?>) indexFactory)::ecommerceTemplate)
				.forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/shop")
	public Index shop(@Bind("q") String query, Long category, String sort) {
//		IO.println("WebHandling.shop, query=" + query + ", category=" + category);
		var i = indexFactory.newIndex();
		i.app().state().put("categories", ((EcommerceApiClient) apiClient).categories().read().elements());
		i.app().state().put("products", ((EcommerceApiClient) apiClient).products()
				.read(null, query, category, sort, 1, tokenCookie()).elements());
		Stream.of("card", "shop").map(((EcommerceIndexFactory<?>) indexFactory)::ecommerceTemplate)
				.forEach(i.templates()::add);
		return i;
	}
}
