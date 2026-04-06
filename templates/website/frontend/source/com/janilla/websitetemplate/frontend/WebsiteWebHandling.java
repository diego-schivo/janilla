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
package com.janilla.websitetemplate.frontend;

import java.util.List;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontendHttpExchange;
import com.janilla.blanktemplate.frontend.BlankWebHandling;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.websitetemplate.Archive;
import com.janilla.websitetemplate.WebsiteDomain;

public class WebsiteWebHandling extends BlankWebHandling {

	protected final WebsiteDomain domain;

	protected final WebsiteDataFetching dataFetching;

	public WebsiteWebHandling(IndexFactory indexFactory, WebsiteDomain domain, WebsiteDataFetching dataFetching) {
		super(indexFactory);
		this.domain = domain;
		this.dataFetching = dataFetching;
	}

	@Override
	public Index home(HttpExchange exchange) {
		return page("home", exchange);
	}

	@Handle(method = "GET", path = "/([\\w\\d-]+)")
	public Index page(String slug, HttpExchange exchange) {
//		IO.println("WebHandling.page, slug=" + slug);
		var pp = dataFetching.pages(slug, 1, ((BlankFrontendHttpExchange) exchange).tokenCookie());
		if (pp.totalSize() == 0) {
			if (slug.equals("home"))
				pp = ListPortion.of(List.of(domain.emptyPage().withSlug("home")));
			else
				throw new NotFoundException("slug=" + slug);
		}
		var i = indexFactory.newIndex(exchange);
		var p = pp.elements().getFirst();
		i.app().state().put("page", p);

		if (p.layout() != null && p.layout().stream().anyMatch(x -> x instanceof Archive))
			i.app().state().put("posts",
					dataFetching.posts(null, 1, ((BlankFrontendHttpExchange) exchange).tokenCookie()).elements());

		Stream.of("archive", "call-to-action", "content", "form-block", "hero", "media-block", "page")
				.map(((WebsiteIndexFactory) indexFactory)::websiteTemplate).forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/posts/([\\w\\d-]+)")
	public Index post(String slug, HttpExchange exchange) {
//		IO.println("WebHandling.post, slug=" + slug);
		var pp = dataFetching.posts(slug, 1, ((BlankFrontendHttpExchange) exchange).tokenCookie());
		if (pp.totalSize() == 0)
			throw new NotFoundException("slug=" + slug);
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("post", pp.elements().getFirst());
		Stream.of("banner", "card", "media-block", "post", "rich-text")
				.map(((WebsiteIndexFactory) indexFactory)::websiteTemplate).forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/posts")
	public Index posts(HttpExchange exchange) {
//		IO.println("WebHandling.posts");
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("posts", dataFetching.posts(null, 1, ((BlankFrontendHttpExchange) exchange).tokenCookie()));
		Stream.of("card", "posts").map(((WebsiteIndexFactory) indexFactory)::websiteTemplate)
				.forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/search")
	public Index search(@Bind("q") String query, HttpExchange exchange) {
//		IO.println("WebHandling.search, query=" + query);
		var i = indexFactory.newIndex(exchange);
		i.app().state().put("results", dataFetching.searchResults(query));
		Stream.of("card", "search").map(((WebsiteIndexFactory) indexFactory)::websiteTemplate)
				.forEach(i.templates()::add);
		return i;
	}
}
