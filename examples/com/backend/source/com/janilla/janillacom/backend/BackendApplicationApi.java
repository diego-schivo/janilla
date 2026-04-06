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
package com.janilla.janillacom.backend;

import java.util.List;
import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpExchange;
import com.janilla.janillacom.Application;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Handle;

@Handle(path = "/api/applications")
public class BackendApplicationApi extends AbstractCollectionApi<Long, Application> {// implements ApplicationApi {

	public BackendApplicationApi(Predicate<HttpExchange> drafts, Persistence persistence) {
		super(Application.class, drafts, persistence, "title");
	}

//	@Override
	@Handle(method = "GET")
	public ListPortion<Application> read(String slug, String search, Boolean reverse, Long skip, Long limit,
			Integer depth) {
		if (slug != null) {
			var ll = crud().filter("slug", new Object[] { slug });
			var a = !ll.isEmpty() ? crud().read(ll.getFirst(), depth != null ? depth : 0) : null;
			return a != null ? new ListPortion<>(List.of(a), 1) : ListPortion.empty();
		}
		return read(search, reverse, skip, limit, depth);
	}
}
