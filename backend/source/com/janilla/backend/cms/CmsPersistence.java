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

import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.persistence.Entity;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.java.TypeResolver;

public class CmsPersistence extends Persistence {

	protected static final DocumentObserver<?> DOCUMENT_OBSERVER = new DocumentObserver<>();

	public CmsPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
			TypeResolver typeResolver) {
		super(database, storables, typeResolver);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
//		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
//				&& type.isAnnotationPresent(Store.class)) {
		@SuppressWarnings("unchecked")
		var t = (Class<? extends Document<?>>) type;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		var c = (Crud<?, E>) new DocumentCrud(t, idConverter(t), this);
		@SuppressWarnings("unchecked")
		var o = (CrudObserver<E>) DOCUMENT_OBSERVER;
		c.observers().add(o);
		return c;
//		}
//		return null;
	}

	@Override
	protected void createStoresAndIndexes() {
		database.perform(() -> {
			super.createStoresAndIndexes();

			for (var t : configuration.cruds().keySet()) {
				var v = t.getAnnotation(Versions.class);
				if (v != null) {
					var n = Version.class.getSimpleName() + "<" + t.getSimpleName() + ">";
					database.createTable(n,
							new TableColumn[] { new TableColumn("id", "INTEGER", true),
									new TableColumn("document", "TEXT", false),
									new TableColumn("documentId", "INTEGER", false) },
							false);
					database.createIndex(n + ".documentId", n, "documentId");
					if (v.drafts())
						for (var k : configuration.indexes()) {
							var ss = k.split("\\.");
							if (ss[0].equals(t.getSimpleName()))
								database.createTable(k + "Draft", new TableColumn[] {
										new TableColumn(ss[1], "TEXT", true), new TableColumn("id", "INTEGER", false) },
										true);
						}
				}
			}

			return null;
		}, true);
	}
}
