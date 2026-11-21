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
import java.util.Set;

import com.janilla.backend.persistence.Crud;
import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.Version;

public interface DocumentCrud<ID extends Comparable<ID>, D extends Document<ID>> extends Crud<ID, D> {

	D read(ID id, boolean drafts, int depth);

	List<D> read(List<ID> ids, boolean drafts, int depth);

	D update(ID id, D document, Set<String> include, boolean newVersion);

	List<D> patch(List<ID> ids, D document, Set<String> include);

	List<Version<ID, D>> readVersions(ID id);

	Version<ID, D> readVersion(ID versionId);

	D restoreVersion(ID versionId, DocumentStatus status);

}
