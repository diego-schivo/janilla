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

import java.util.Set;

import com.janilla.json.MapAndType;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;

public abstract class GlobalApi<E> {

	protected final Class<E> type;

	public Persistence persistence;

	protected GlobalApi(Class<E> type) {
		this.type = type;
	}

	@Handle(method = "GET")
	public E read() {
		var e = crud().read(1);
		if (e == null)
			throw new NotFoundException("entity " + 1);
		return e;
	}

	@Handle(method = "PUT")
	public E update(@Bind(resolver = MapAndType.DollarTypeResolver.class) E entity) {
		var e = crud().update(1, x -> Reflection.copy(entity, x, y -> !Set.of("id").contains(y)));
		if (e == null)
			throw new NotFoundException("entity " + 1);
		return e;
	}

	protected Crud<E> crud() {
		return persistence.crud(type);
	}
}
