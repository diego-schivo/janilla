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

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.janilla.java.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.CrudObserver;
import com.janilla.persistence.Entity;
import com.janilla.persistence.Persistence;
import com.janilla.persistence.Store;
import com.janilla.reflect.Reflection;
import com.janilla.sqlite.SqliteDatabase;
import com.janilla.sqlite.TableColumn;

public class CmsPersistence extends Persistence {

	protected static final CrudObserver DOCUMENT_OBSERVER = new CrudObserver() {

		@Override
		public Entity beforeCreate(Entity entity) {
			var d = (Document<?>) entity;
			var i = Instant.now();
			var v = d.getClass().getAnnotation(Versions.class);
			var m = Map.<String, Object>of("createdAt", i, "updatedAt", i);
			if (d.documentStatus() == null) {
				m = new HashMap<>(m);
				m.put("documentStatus", v != null && v.drafts() ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED);
			}
			return Reflection.copy(m, entity);
		}

		@Override
		public Entity beforeUpdate(Entity entity) {
			var d = (Document<?>) entity;
			var i = Instant.now();
			var m = Map.<String, Object>of("updatedAt", i);
			if (d.documentStatus() == DocumentStatus.PUBLISHED) {
				m = new HashMap<>(m);
				m.put("publishedAt", i);
			}
			return Reflection.copy(m, entity);
		}
	};

	public CmsPersistence(SqliteDatabase database, Collection<Class<? extends Entity<?>>> types,
			TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

//	@Override
//	protected <E extends Entity<?>, K, V> void configure(Class<E> type) {
//		super.configure(type);
//		var v = type.getAnnotation(Versions.class);
//		if (v != null && v.drafts())
//			for (var k : configuration.indexes().toArray(String[]::new)) {
//				var ss = k.split("_");
//				if (ss[0].equals(type.getSimpleName()))
//					configuration.indexes().add(k + "Draft");
//			}
//	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class)) {
			@SuppressWarnings("unchecked")
			var t = (Class<? extends Document<?>>) type;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var c = (Crud<?, E>) new DocumentCrud(t, idConverter(t), this);
			c.observers().add(DOCUMENT_OBSERVER);
			return c;
		}
		return null;
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
