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
package com.janilla.websitetemplate.backend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Handle;
import com.janilla.websitetemplate.Page;

@Handle(path = "/api/pages")
public class PageApi extends AbstractCollectionApi<Long, Page> {

	private static final Logger LOGGER = System.getLogger(PageApi.class.getName());

	public PageApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier) {
		super(Page.class, drafts, persistence, "title", copier, Direction.FORWARD, 0);
	}

	@Handle(method = "GET")
	public ListPortion<Page> read(String search, Direction direction, Long skip, Long limit, Integer depth, String slug,
			HttpExchange exchange) {
		LOGGER.log(Level.DEBUG, "search={0}, direction={1}, skip={2}, limit={3}, depth={4}, slug={5}", search, direction,
				skip, limit, depth, slug);

		if (slug != null && !slug.isBlank()) {
			var i = drafts.test(exchange) ? "slugDraft" : "slug";
			var d = depth != null ? depth.intValue() : defaultDepth;
			var p = crud().read(crud().find(i, new Object[] { slug }), d);

			return p != null ? ListPortion.of(List.of(p)) : ListPortion.empty();
		}

		return super.read(search, direction, skip, limit, depth);
	}
}
