/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.janillacom.frontend;

import java.net.URI;
import java.util.List;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.janillacom.Application;
import com.janilla.java.Configuration;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.frontend.WebsiteDataFetching;

public class JanillaDataFetching extends WebsiteDataFetching {

	public JanillaDataFetching(Configuration configuration, String configurationKey, HttpClient httpClient,
			Converter converter) {
		super(configuration, configurationKey, httpClient, converter);
	}

	public ListPortion<Application> applications(String slug, String search, Boolean reverse, Long skip, Long limit,
			Integer depth) {
		var u = URI.create(apiUrl + "/applications?"
				+ new UriQueryBuilder().append("slug", slug).append("search", search)
						.append("reverse", reverse != null ? reverse.toString() : null)
						.append("skip", skip != null ? skip.toString() : null)
						.append("limit", limit != null ? limit.toString() : null)
						.append("depth", depth != null ? depth.toString() : null));
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(Application.class)));
	}
}
