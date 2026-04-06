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

import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.Document;
import com.janilla.websitetemplate.SearchResult;

public class SearchObserver<D extends Document<?>> implements CrudObserver<D> {

	protected final List<Class<?>> types;

	protected final Persistence persistence;

	public SearchObserver(List<Class<?>> types, Persistence persistence) {
		this.types = types;
		this.persistence = persistence;
	}

//	@Override
//	public void afterCreate(D entity) {
//		var d = (Document<?>) entity;
//		var dc = d.getClass();
//		if (types.contains(dc) && d.documentStatus() == DocumentStatus.PUBLISHED) {
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			var r = new DocumentReference(dc, d.id());
//			crud().create(Reflection.copy(d, new SearchResult(null, r, null, null, null, null, null, null, null, null),
//					y -> !Set.of("id", "document").contains(y)));
//		}
//	}
//
//	@Override
//	public void afterUpdate(D entity1, D entity2) {
//		var d1 = (Document<?>) entity1;
//		var d2 = (Document<?>) entity2;
//		var dc = d1.getClass();
//		if (types.contains(dc)) {
//			switch (d1.documentStatus()) {
//			case DRAFT:
//				if (d2.documentStatus() == d1.documentStatus())
//					;
//				else {
//					@SuppressWarnings({ "rawtypes", "unchecked" })
//					var r = new DocumentReference(dc, d2.id());
//					crud().create(Reflection.copy(d2,
//							new SearchResult(null, r, null, null, null, null, null, null, null, null),
//							y -> !Set.of("id", "document").contains(y)));
//				}
//				break;
//			case PUBLISHED:
//				if (d2.documentStatus() == d1.documentStatus()) {
//					@SuppressWarnings({ "rawtypes", "unchecked" })
//					var r = new DocumentReference(dc, d2.id());
//					crud().update(crud().find("document", new Object[] { r }),
//							x -> Reflection.copy(d2, x, y -> !Set.of("id", "document").contains(y)));
//				} else
//					crud().delete(crud().find("document", new Object[] { d2.id() }));
//				break;
//			}
//		}
//	}
//
//	@Override
//	public void afterDelete(D entity) {
//		var d = (Document<?>) entity;
//		var dc = d.getClass();
//		if (types.contains(dc) && d.documentStatus() == DocumentStatus.PUBLISHED) {
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			var r = new DocumentReference(dc, d.id());
//			crud().delete(crud().find("document", new Object[] { r }));
//		}
//	}

	protected Crud<Long, SearchResult> crud() {
		return persistence.crud(SearchResult.class);
	}
}
