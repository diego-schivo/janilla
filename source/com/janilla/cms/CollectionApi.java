/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
package com.janilla.cms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.janilla.http.HttpExchange;
import com.janilla.json.MapAndType;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.MethodHandlerFactory;
import com.janilla.web.NotFoundException;

public abstract class CollectionApi<ID extends Comparable<ID>, E extends Document<ID>> {

	protected final Class<E> type;

	protected final Predicate<HttpExchange> drafts;

	public Persistence persistence;

	protected CollectionApi(Class<E> type, Predicate<HttpExchange> drafts) {
		this.type = type;
		this.drafts = drafts;
	}

	@Handle(method = "POST")
	public E create(@Bind(resolver = MapAndType.DollarTypeResolver.class) E entity) {
		return crud().create(entity);
	}

	@Handle(method = "GET")
	public List<E> read() {
		return crud().read(crud().list());
	}

	@Handle(method = "GET", path = "(\\d+)")
	public E read(ID id, HttpExchange exchange) {
		var e = crud().read(id, drafts.test(exchange));
		if (e == null)
			throw new NotFoundException("entity " + id);
		return e;
	}

	@Handle(method = "PUT", path = "(\\d+)")
	public E update(ID id, @Bind(resolver = MapAndType.DollarTypeResolver.class) E entity, Boolean draft,
			Boolean autosave) {
		var s = Boolean.TRUE.equals(draft) ? Document.Status.DRAFT : Document.Status.PUBLISHED;
		if (s != entity.documentStatus())
			entity = Reflection.copy(Map.of("documentStatus", s), entity);
		var e = crud().update(id, entity, updateInclude(entity), !Boolean.TRUE.equals(autosave));
		if (e == null)
			throw new NotFoundException("entity " + id);
		return e;
	}

	@Handle(method = "DELETE", path = "(\\d+)")
	public E delete(ID id) {
		var e = crud().delete(id);
		if (e == null)
			throw new NotFoundException("entity " + id);
		return e;
	}

	@Handle(method = "DELETE")
	public List<E> delete(@Bind("id") List<ID> ids) {
		return crud().delete(ids);
	}

	@Handle(method = "PATCH", path = "(\\d+)")
	public E patch(ID id, @Bind(resolver = MapAndType.DollarTypeResolver.class) E entity) {
		return patch(entity, List.of(id)).getFirst();
	}

	@Handle(method = "PATCH")
	public List<E> patch(@Bind(resolver = MapAndType.DollarTypeResolver.class) E entity, @Bind("id") List<ID> ids) {
		return crud().patch(ids, entity, MethodHandlerFactory.JSON_KEYS.get());
	}

	@Handle(method = "GET", path = "(\\d+)/versions")
	public List<Version<ID, E>> readVersions(ID id) {
		return crud().readVersions(id);
	}

	@Handle(method = "GET", path = "versions/(\\d+)")
	public Version<ID, E> readVersion(ID versionId) {
		return crud().readVersion(versionId);
	}

	@Handle(method = "POST", path = "versions/(\\d+)")
	public E restoreVersion(ID versionId, Boolean draft) {
		return crud().restoreVersion(versionId,
				Boolean.TRUE.equals(draft) ? Document.Status.DRAFT : Document.Status.PUBLISHED);
	}

	protected DocumentCrud<ID, E> crud() {
		return (DocumentCrud<ID, E>) persistence.crud(type);
	}

	protected Set<String> updateInclude(E entity) {
		return null;
	}
}
