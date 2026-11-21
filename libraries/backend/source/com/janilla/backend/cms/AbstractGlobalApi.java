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
package com.janilla.backend.cms;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.GlobalApi;
import com.janilla.cms.Version;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.DollarTypeResolver;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

public abstract class AbstractGlobalApi<ID extends Comparable<ID>, D extends Document<ID>> implements GlobalApi<ID, D> {

	protected final Copier copier;

	protected final Predicate<HttpExchange> drafts;

	protected final ID id;

	protected final Persistence persistence;

	protected final Class<D> type;

	protected AbstractGlobalApi(Class<D> type, Predicate<HttpExchange> drafts, Persistence persistence, Copier copier,
			ID id) {
		this.type = type;
		this.drafts = drafts;
		this.persistence = persistence;
		this.copier = copier;
		this.id = id;
	}

	@Override
	@Handle(method = "POST")
	public D create(@Bind(resolver = DollarTypeResolver.class) D document) {
		return crud().create(document);
	}

	@Override
	@Handle(method = "GET")
	public D read(Integer depth, HttpExchange exchange) {
		return crud().read(id, drafts.test(exchange), depth != null ? depth : 0);
	}

	@Override
	@Handle(method = "PUT")
	public D update(@Bind(resolver = DollarTypeResolver.class) D document, Boolean draft, Boolean autosave) {
		var s = Boolean.TRUE.equals(draft) ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED;
		if (s != document.documentStatus())
			document = copier.copy(Map.of("documentStatus", s), document);
		return crud().update(id, document, null, !Boolean.TRUE.equals(autosave));
	}

	@Override
	@Handle(method = "DELETE")
	public D delete() {
		return crud().delete(id);
	}

	@Override
	@Handle(method = "GET", path = "versions")
	public List<Version<ID, D>> readVersions() {
		return crud().readVersions(id);
	}

	@Override
	@Handle(method = "GET", path = "versions/(\\d+)")
	public Version<ID, D> readVersion(ID versionId) {
		return crud().readVersion(versionId);
	}

	@Override
	@Handle(method = "POST", path = "versions/(\\d+)")
	public D restoreVersion(ID versionId, Boolean draft) {
		return crud().restoreVersion(versionId,
				Boolean.TRUE.equals(draft) ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED);
	}

	protected DefaultDocumentCrud<ID, D> crud() {
		return (DefaultDocumentCrud<ID, D>) persistence.crud(type);
	}
}
