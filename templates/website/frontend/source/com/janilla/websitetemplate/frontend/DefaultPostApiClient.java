/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Payload CMS, LLC <info@payloadcms.com>
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
package com.janilla.websitetemplate.frontend;

import java.lang.reflect.Type;
import java.net.URI;

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.DefaultHttpRequest;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpCookie;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.Post;

public class DefaultPostApiClient implements PostApiClient {

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	protected final Converter converter;

	public DefaultPostApiClient(FrontendConfig config, HttpClient httpClient, Converter converter) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
	}

	@Override
	public ListPortion<Post> read(String slug, Integer depth, HttpCookie token) {
		var r = new DefaultHttpRequest("GET", URI.create(config.api().url() + "/posts?"
				+ new UriQueryBuilder().append("slug", slug).append("depth", depth != null ? depth.toString() : null)),
				token != null ? token.format() : null);
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { Post.class }));
	}

}
