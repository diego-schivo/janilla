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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.backend.cms;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.CollectionApi;
import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.Version;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.java.DollarTypeResolver;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.InvocationHandlerFactory;

public abstract class AbstractCollectionApi<ID extends Comparable<ID>, D extends Document<ID>>
		implements CollectionApi<ID, D> {

	private static final Logger LOGGER = System.getLogger(AbstractCollectionApi.class.getName());

	protected final Copier copier;

	protected final int defaultDepth;

	protected final Direction defaultDirection;

	protected final Predicate<HttpExchange> drafts;

	protected final Persistence persistence;

	protected final String searchIndex;

	protected final Class<D> type;

	protected AbstractCollectionApi(Class<D> type, Predicate<HttpExchange> drafts, Persistence persistence,
			String searchIndex, Copier copier, Direction defaultDirection, Integer defaultDepth) {
		this.type = type;
		this.drafts = drafts;
		this.persistence = persistence;
		this.searchIndex = searchIndex;
		this.copier = copier;
		this.defaultDirection = defaultDirection != null ? defaultDirection : Direction.FORWARD;
		this.defaultDepth = defaultDepth != null ? defaultDepth : 0;
	}

	@Override
	@Handle(method = "POST")
	public D create(@Bind(resolver = DollarTypeResolver.class) D document) {
		return crud().create(document);
	}

	@Override
	@Handle(method = "GET", path = "([^/]+)")
	public D read(ID id, Integer depth) {
		return crud().read(id, drafts.test(HttpExchange.SCOPED.get()), depth != null ? depth : defaultDepth);
	}

	@Override
	@Handle(method = "GET")
	public ListPortion<D> read(String search, Direction direction, Long skip, Long limit, Integer depth) {
		LOGGER.log(Level.DEBUG, "search={0}, direction={1}, skip={2}, limit={3}, depth={4}", search, direction, skip,
				limit, depth);

		var d = direction != null ? direction : defaultDirection;
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var p = depth != null ? depth.intValue() : defaultDepth;

		var s = search != null && !search.isBlank() ? search.strip().toLowerCase() : null;
		var ii = s != null ? crud().filterAndCount(searchIndex, x -> ((String) x).toLowerCase().contains(s), d, k, l)
				: crud().listAndCount(d, k, l);
		return ii.map(x -> crud().read(x, p));
	}

	@Override
	@Handle(method = "PUT", path = "([^/]+)")
	public D update(ID id, @Bind(resolver = DollarTypeResolver.class) D document, Boolean draft, Boolean autosave) {
		LOGGER.log(Level.DEBUG, "id={0}, document={1}, draft={2}, autosave={3}", id, document, draft, autosave);

		var s = draft != null && draft.booleanValue() ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED;
		if (s != document.documentStatus())
			document = copier.copy(Map.of("documentStatus", s), document);

		var nv = !(autosave != null && autosave.booleanValue());
		var d = crud().update(id, document, updateInclude(document), nv);
		LOGGER.log(Level.DEBUG, "d={0}", d);

		return d;
	}

	@Override
	@Handle(method = "DELETE", path = "([^/]+)")
	public D delete(ID id) {
		return crud().delete(id);
	}

	@Override
	@Handle(method = "DELETE")
	public List<D> delete(@Bind("id") List<ID> ids) {
		return crud().delete(ids);
	}

	@Override
	@Handle(method = "PATCH", path = "([^/]+)")
	public D patch(ID id, @Bind(resolver = DollarTypeResolver.class) D document) {
		return patch(document, List.of(id)).getFirst();
	}

	@Override
	@Handle(method = "PATCH")
	public List<D> patch(@Bind(resolver = DollarTypeResolver.class) D document, @Bind("id") List<ID> ids) {
		return crud().patch(ids, document, InvocationHandlerFactory.JSON_KEYS.get());
	}

	@Override
	@Handle(method = "GET", path = "([^/]+)/versions")
	public List<Version<ID, D>> readVersions(ID id) {
		return crud().readVersions(id);
	}

	@Override
	@Handle(method = "GET", path = "versions/([^/]+)")
	public Version<ID, D> readVersion(ID versionId) {
		return crud().readVersion(versionId);
	}

	@Override
	@Handle(method = "POST", path = "versions/([^/]+)")
	public D restoreVersion(ID versionId, Boolean draft) {
		return crud().restoreVersion(versionId,
				Boolean.TRUE.equals(draft) ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED);
	}

	protected DocumentCrud<ID, D> crud() {
		return (DocumentCrud<ID, D>) persistence.crud(type);
	}

	protected Set<String> updateInclude(D document) {
		return null;
	}
}
