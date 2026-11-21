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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.HashMap;

import com.janilla.backend.persistence.CrudObserver;
import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.Versions;
import com.janilla.java.Copier;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;

public class DocumentObserver<D extends Document<?>> implements CrudObserver<D> {

	private static final Logger LOGGER = System.getLogger(DocumentObserver.class.getName());

	protected final Copier copier;

	public DocumentObserver(Copier copier) {
		this.copier = copier;
	}

	@Override
	public D beforeCreate(D document) {
		LOGGER.log(Level.DEBUG, "document={0}", document);

		var m = new HashMap<String, Object>(3);

		var i = Instant.now();
		if (document.createdAt() == null)
			m.put("createdAt", i);

		if (document.updatedAt() == null)
			m.put("updatedAt", i);

		if (document.documentStatus() == null) {
			var aa = JavaReflect.inheritedAnnotation(document.getClass(), Versions.class);
			var v = aa != null ? aa.annotation() : null;
			m.put("documentStatus", v != null && v.drafts() ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED);
		}

		var d = copier.copy(m, document);
		LOGGER.log(Level.DEBUG, "d={0}", d);

		return d;
	}

	@Override
	public D beforeUpdate(D document) {
		LOGGER.log(Level.DEBUG, "document={0}", document);

		var i = Instant.now();
		var m = Java.<String, Object>hashMap("updatedAt", i);

		if (document.documentStatus() == DocumentStatus.PUBLISHED)
			m.put("publishedAt", i);

		var d = copier.copy(m, document);
		LOGGER.log(Level.DEBUG, "d={0}", d);

		return d;
	}
}
