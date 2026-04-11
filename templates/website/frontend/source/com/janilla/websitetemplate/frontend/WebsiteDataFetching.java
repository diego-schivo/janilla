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

import java.net.URI;
import java.util.List;

import com.janilla.frontend.cms.CmsDataFetching;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpRequest;
import com.janilla.java.Configuration;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.Footer;
import com.janilla.websitetemplate.Header;
import com.janilla.websitetemplate.Page;
import com.janilla.websitetemplate.Post;
import com.janilla.websitetemplate.SearchResult;

public class WebsiteDataFetching extends CmsDataFetching {

	public WebsiteDataFetching(Configuration configuration, String configurationKey, HttpClient httpClient,
			Converter converter) {
		super(configuration, configurationKey, httpClient, converter);
	}

	public Footer footer() {
		var r = new HttpRequest("GET", URI.create(apiUrl + "/footer"));
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, Footer.class);
	}

	public Header header(Integer depth) {
		var r = new HttpRequest("GET", URI.create(
				apiUrl + "/header?" + new UriQueryBuilder().append("depth", depth != null ? depth.toString() : null)));
		var o = httpClient.send(r, HttpClient.JSON);
//		IO.println("o=" + o);
		return converter.convert(o, Header.class);
	}

	public ListPortion<Page> pages(String slug, Integer depth, HttpCookie token) {
		var r = new HttpRequest("GET", URI.create(apiUrl + "/pages?"
				+ new UriQueryBuilder().append("slug", slug).append("depth", depth != null ? depth.toString() : null)),
				token != null ? token.format() : null);
		var o = httpClient.send(r, HttpClient.JSON);
//		IO.println("o=" + o);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(Page.class)));
	}

	public ListPortion<Post> posts(String slug, Integer depth, HttpCookie token) {
		var r = new HttpRequest("GET", URI.create(apiUrl + "/posts?"
				+ new UriQueryBuilder().append("slug", slug).append("depth", depth != null ? depth.toString() : null)),
				token != null ? token.format() : null);
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(Post.class)));
	}

	public ListPortion<SearchResult> searchResults(String query) {
		var r = new HttpRequest("GET",
				URI.create(apiUrl + "/search-results?" + new UriQueryBuilder().append("query", query)));
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(SearchResult.class)));
	}
}
