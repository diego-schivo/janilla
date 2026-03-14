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
package com.janilla.frontend.cms;

import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.AbstractIndexFactory;
import com.janilla.web.ResourceMap;

public abstract class CmsIndexFactory extends AbstractIndexFactory {

	protected final CmsDataFetching dataFetching;

	protected CmsIndexFactory(ResourceMap resourceMap, CmsDataFetching dataFetching) {
		super(resourceMap);
		this.dataFetching = dataFetching;
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);

		Stream.of("admin", "admin-array", "admin-bar", "admin-checkbox", "admin-create-first-user", "admin-dashboard",
				"admin-document", "admin-drawer", "admin-drawer-link", "admin-edit", "admin-fields", "admin-file",
				"admin-forgot-password", "admin-hidden", "admin-join", "admin-list", "admin-login",
				"admin-page-controls", "admin-pagination", "admin-per-page", "admin-radio-group", "admin-relationship",
				"admin-rich-text", "admin-search-bar", "admin-search-filter", "admin-select", "admin-slug",
				"admin-tabs", "admin-text", "admin-unauthorized", "admin-upload", "admin-version", "admin-versions")
				.map(this::cmsImportKey).forEach(x -> map.put(x, "/" + x + ".js"));
	}

	protected String cmsImportKey(String name) {
		return name;
	}
}
