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
import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.websitetemplate.Archive;
import com.janilla.websitetemplate.WebsiteDomain;

public class WebsiteWebHandling<D extends WebsiteDomain, C extends WebsiteApiClient> extends BlankWeb<D, C> {

	public WebsiteWebHandling(IndexFactory indexFactory, D domain, C apiClient) {
		super(indexFactory, domain, apiClient);
	}

	@Override
	public Index home() {
		return page("home");
	}

	@Handle(method = "GET", path = "/([\\w\\d-]+)")
	public Index page(String slug) {
//		IO.println("WebHandling.page, slug=" + slug);
		var pp = apiClient.pages().read(slug, 1, tokenCookie());
		if (pp.totalSize() == 0) {
			if (slug.equals("home"))
				pp = ListPortion.of(List.of(domain.emptyPage().withSlug("home")));
			else
				throw new NotFoundException("slug=" + slug);
		}
		var i = indexFactory.newIndex();
		var p = pp.elements().getFirst();
		i.app().state().put("page", p);

		if (p.layout() != null && p.layout().stream().anyMatch(x -> x instanceof Archive))
			i.app().state().put("posts", apiClient.posts().read(null, 1, tokenCookie()).elements());

		Stream.of("archive", "call-to-action", "content", "form-block", "hero", "media-block", "page")
				.map(((WebsiteIndexFactory<?>) indexFactory)::websiteTemplate).forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/posts/([\\w\\d-]+)")
	public Index post(String slug) {
//		IO.println("WebHandling.post, slug=" + slug);
		var pp = apiClient.posts().read(slug, 1, tokenCookie());
		if (pp.totalSize() == 0)
			throw new NotFoundException("slug=" + slug);
		var i = indexFactory.newIndex();
		i.app().state().put("post", pp.elements().getFirst());
		Stream.of("banner", "card", "media-block", "post", "rich-text")
				.map(((WebsiteIndexFactory<?>) indexFactory)::websiteTemplate).forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/posts")
	public Index posts() {
//		IO.println("WebHandling.posts");
		var i = indexFactory.newIndex();
		i.app().state().put("posts", apiClient.posts().read(null, 1, tokenCookie()));
		Stream.of("card", "posts").map(((WebsiteIndexFactory<?>) indexFactory)::websiteTemplate)
				.forEach(i.templates()::add);
		return i;
	}

	@Handle(method = "GET", path = "/search")
	public Index search(@Bind("q") String query) {
//		IO.println("WebHandling.search, query=" + query);
		var i = indexFactory.newIndex();
		i.app().state().put("results", apiClient.searchResults().read(query));
		Stream.of("card", "search").map(((WebsiteIndexFactory<?>) indexFactory)::websiteTemplate)
				.forEach(i.templates()::add);
		return i;
	}

	protected HttpCookie tokenCookie() {
		return ((BlankFrontendHttpExchange) HttpExchange.SCOPED.get()).tokenCookie();
	}
}
