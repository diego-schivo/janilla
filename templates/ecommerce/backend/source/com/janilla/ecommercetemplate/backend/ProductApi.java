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

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.ecommercetemplate.Product;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/products")
public class ProductApi extends AbstractCollectionApi<Long, Product> {

	public ProductApi(Predicate<HttpExchange> drafts, Persistence persistence) {
		super(Product.class, drafts, persistence, "title");
	}

	@Handle(method = "GET")
	public ListPortion<Product> read(String search, Boolean reverse, Long skip, Long limit, Integer depth, String slug,
			@Bind("q") String query, @Bind("category") Long[] categories, String sort, HttpExchange exchange) {
		{
			var s = slug != null && !slug.isEmpty() ? slug : null;
			if (s != null) {
				var p = crud().read(crud().find(drafts.test(exchange) ? "slugDraft" : "slug", new Object[] { slug }),
						depth != null ? depth : 0);
				return p != null ? new ListPortion<>(List.of(p), 1) : ListPortion.empty();
			}
		}

		{
			var cc = categories != null && categories.length != 0 ? categories : null;
			var q = query != null && !query.isBlank() ? query.strip().toLowerCase() : null;
			var s = sort != null && !sort.isEmpty() ? sort : null;
			if (cc != null || q != null || s != null) {
				var pp = crud().read(cc != null ? crud().filter("categories", cc) : crud().list()).stream();

				pp = q != null ? pp.filter(x -> {
					var m = x.meta();
					var t = Stream.of(m != null ? m.title() : null, m != null ? m.description() : null)
							.filter(y -> y != null && !y.isBlank()).collect(Collectors.joining(" "));
					return t.toLowerCase().contains(q);
				}) : pp;

				switch (s != null ? s : "") {
				case "":
					pp = pp.sorted(Comparator.comparing(x -> x.title()));
					break;
				case "-createdAt":
					pp = pp.sorted(Comparator.comparing((Product x) -> x.createdAt()).reversed());
					break;
				case "priceInUsd":
					pp = pp.sorted(Comparator.comparing(x -> x.priceInUsd()));
					break;
				case "-priceInUsd":
					pp = pp.sorted(Comparator.comparing((Product x) -> x.priceInUsd()).reversed());
					break;
				}

				var d = depth != null ? depth.intValue() : 0;
				return ListPortion.of(d > 0 ? crud().read(pp.map(x -> x.id()).toList(), d) : pp.toList());
			}
		}

		return super.read(search, reverse, skip, limit, depth);
	}
}
