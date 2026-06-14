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

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

public class WebsiteApiClient extends BlankApiClient {

	protected final FooterApiClient footer;

	protected final HeaderApiClient header;

	protected final PageApiClient pages;

	protected final PostApiClient posts;

	protected final SearchResultApiClient searchResults;

	public WebsiteApiClient(DiFactory diFactory) {
		super(diFactory);

		footer = diFactory.newInstance(diFactory.classFor(FooterApiClient.class));
		header = diFactory.newInstance(diFactory.classFor(HeaderApiClient.class));
		pages = diFactory.newInstance(diFactory.classFor(PageApiClient.class));
		posts = diFactory.newInstance(diFactory.classFor(PostApiClient.class));
		searchResults = diFactory.newInstance(diFactory.classFor(SearchResultApiClient.class));
	}

	public FooterApiClient footer() {
		return footer;
	}

	public HeaderApiClient header() {
		return header;
	}

	public PageApiClient pages() {
		return pages;
	}

	public PostApiClient posts() {
		return posts;
	}

	public SearchResultApiClient searchResults() {
		return searchResults;
	}

}
