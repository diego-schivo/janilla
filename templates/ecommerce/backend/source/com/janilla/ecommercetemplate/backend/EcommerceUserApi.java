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

import java.util.Set;
import java.util.function.Predicate;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.web.BackendConfig;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.web.Handle;
import com.janilla.websitetemplate.backend.WebsiteUserApi;

public class EcommerceUserApi extends WebsiteUserApi {

	public EcommerceUserApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier,
			BackendConfig config, EcommerceDomain domain) {
		super(drafts, persistence, copier, config, domain);
	}

	@Handle(method = "POST")
	public User<Long> create(UserData<User<Long>> data) {
		if (exchange().sessionUser() == null) {
			var u = domain.withRoles(data.user(), Set.of(domain.userRole("CUSTOMER")));
			data = data.withUser(u);
		}

		return super.create(data);
	}
}
