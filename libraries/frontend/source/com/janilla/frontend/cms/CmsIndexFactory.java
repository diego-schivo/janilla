/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.frontend.cms;

import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.ApiClient;
import com.janilla.frontend.DefaultIndexFactory;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.ioc.DiFactory;
import com.janilla.web.ResourceMap;

public abstract class CmsIndexFactory<C extends FrontendConfig> extends DefaultIndexFactory<C> {

	protected final ApiClient apiClient;

	protected CmsIndexFactory(C config, ResourceMap resourceMap, DiFactory diFactory, ApiClient apiClient) {
		super(config, resourceMap, diFactory);

		this.apiClient = apiClient;
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);

		Stream.of("admin", "admin-array", "admin-bar", "admin-checkbox", "admin-create-first-user", "admin-dashboard",
				"admin-date-time", "admin-document", "admin-drawer", "admin-drawer-link", "admin-edit", "admin-fields",
				"admin-file", "admin-forgot-password", "admin-hidden", "admin-join", "admin-list", "admin-login",
				"admin-nav-toggler", "admin-page-controls", "admin-pagination", "admin-password", "admin-per-page",
				"admin-radio-group", "admin-relationship", "admin-rich-text", "admin-search-bar", "admin-search-filter",
				"admin-select", "admin-slug", "admin-tabs", "admin-text", "admin-unauthorized", "admin-upload",
				"admin-version", "admin-versions").map(this::cmsImportKey)
				.forEach(x -> map.put(x, config.basePath() + "/" + x + ".js"));
	}

	protected String cmsImportKey(String name) {
		return name;
	}
}
