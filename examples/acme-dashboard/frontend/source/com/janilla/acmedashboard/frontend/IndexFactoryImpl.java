/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
package com.janilla.acmedashboard.frontend;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.frontend.Index;
import com.janilla.frontend.AbstractIndexFactory;
import com.janilla.frontend.Template;
import com.janilla.http.HttpExchange;
import com.janilla.web.ResourceMap;

public class IndexFactoryImpl extends AbstractIndexFactory {

	protected final Properties configuration;

	public IndexFactoryImpl(ResourceMap resourceMap, Properties configuration) {
		super(resourceMap);
		this.configuration = configuration;
	}

	@Override
	public Index newIndex(HttpExchange exchange) {
		return new IndexImpl("Acme Dashboard", imports(), scripts(),
				new AppImpl(configuration.getProperty("acme-dashboard.api.url"), state(exchange)), templates());
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);
		Stream.of("acme-logo", "app", "breadcrumb-nav", "card-wrapper", "customers-page", "dashboard-page",
				"dashboard-layout", "dashboard-nav", "hero-icon", "invoice-page", "invoice-status", "invoices-layout",
				"invoices-page", "latest-invoices", "login-page", "pagination-nav", "revenue-chart", "single-card",
				"welcome-page").forEach(x -> map.put(x, "/" + x + ".js"));
	}

	@Override
	protected String baseImportKey(String name) {
		return "base/" + name;
	}

	@Override
	protected void addTemplates(List<Template> list) {
		Stream.of("acme-logo", "app", "breadcrumb-nav", "card-wrapper", "customers-page", "dashboard-page",
				"dashboard-layout", "dashboard-nav", "hero-icon", "invoice-page", "invoice-status", "invoices-layout",
				"invoices-page", "latest-invoices", "login-page", "pagination-nav", "revenue-chart", "single-card",
				"welcome-page").map(this::template).forEach(list::add);
	}
}
