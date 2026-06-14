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

import java.util.Arrays;
import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.blanktemplate.backend.BlankPersistence;
import com.janilla.cms.Types;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Copier;
import com.janilla.java.JavaReflect;
import com.janilla.persistence.Entity;
import com.janilla.websitetemplate.SearchResult;

public class WebsitePersistence<C extends WebsiteBackendConfig> extends BlankPersistence<C> {

//	private static final Logger LOGGER = System.getLogger(WebsitePersistence.class.getName());

	private SearchObserver<?> searchObserver;

	public WebsitePersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, DiFactory diFactory,
			C config, Class<?> seedDataClass, Copier copier) {
		super(database, storables, diFactory, config, seedDataClass, copier);
	}

	protected SearchObserver<?> searchObserver() {
		if (searchObserver == null)
			searchObserver = new SearchObserver<>(Arrays.stream(JavaReflect.property(SearchResult.class, "document")
					.annotatedType().getAnnotation(Types.class).value()).toList(), this);
		return searchObserver;
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			@SuppressWarnings("unchecked")
			var o = (CrudObserver<E>) searchObserver();
			c.observers().add(o);
		}
		return c;
	}
}
