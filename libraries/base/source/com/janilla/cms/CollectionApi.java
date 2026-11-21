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
package com.janilla.cms;

import java.util.List;

import com.janilla.java.Direction;
import com.janilla.persistence.ListPortion;

public interface CollectionApi<ID extends Comparable<ID>, D extends Document<ID>> {

	D create(D document);

	D delete(ID id);

	List<D> delete(List<ID> ids);

	D patch(ID id, D document);

	List<D> patch(D document, List<ID> ids);

	D read(ID id, Integer depth);

	ListPortion<D> read(String search, Direction direction, Long skip, Long limit, Integer depth);

	Version<ID, D> readVersion(ID versionId);

	List<Version<ID, D>> readVersions(ID id);

	D restoreVersion(ID versionId, Boolean draft);

	D update(ID id, D document, Boolean draft, Boolean autosave);
}
