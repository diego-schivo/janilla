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
package com.janilla.backend.persistence;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.java.Converter;
import com.janilla.java.JavaReflect;
import com.janilla.persistence.Entity;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

public class Persistence {

	protected final SqliteDatabase database;

	protected final List<Class<? extends Entity<?>>> storables;

//	protected final TypeResolver typeResolver;

	protected final Converter converter;

	protected final PersistenceConfiguration configuration = new PersistenceConfiguration();

	protected final Map<Class<?>, Object> cruds = new ConcurrentHashMap<>();

	public Persistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
//			TypeResolver typeResolver,
			Converter converter) {
		this.database = database;
		this.storables = storables;
//		this.typeResolver = typeResolver;
		this.converter = converter;
		for (var t : storables)
			configure(t);
		if (database.schema().isEmpty())
			createStoresAndIndexes();
	}

	public SqliteDatabase database() {
		return database;
	}

//	public TypeResolver typeResolver() {
//		return typeResolver;
//	}

	public Converter converter() {
		return converter;
	}

	public <ID extends Comparable<ID>, E extends Entity<ID>> Crud<ID, E> crud(Class<E> type) {
//		IO.println("Persistence.crud, type=" + type);
		var o = cruds.computeIfAbsent(type, _ -> {
//			var c = configuration.cruds.get(type);
//			if (c == null)
//				c = configuration.cruds.entrySet().stream().filter(x -> type.isAssignableFrom(x.getKey())).findFirst()
//						.map(x -> x.getValue()).orElse(null);
			var ae = JavaReflect.inheritedAnnotation(type, Store.class);
			var c = ae != null ? configuration.cruds.get(ae.annotated()) : null;
			return c != null ? c : new IllegalArgumentException("type=" + type);
		});
		if (o instanceof RuntimeException e)
			throw e;
		@SuppressWarnings("unchecked")
		var c = (Crud<ID, E>) o;
		return c;
	}

	protected <E extends Entity<?>, K, V> void configure(Class<E> type) {
//		IO.println("Persistence.configure, type=" + type);
		Crud<?, E> c = newCrud(type);
		if (c == null)
			return;
		configuration.cruds.put(type, c);

		for (var pp = JavaReflect.properties(type).iterator(); pp.hasNext();) {
			var p = pp.next();
//			IO.println("Persistence.configure, p=" + p);
			var i = JavaReflect.inheritedAnnotation((Method) p.member(), Index.class);
			if (i == null)
				continue;

			var n = Stream.of(!i.name().isEmpty() ? i.name() : null,
					i.properties().length != 0 ? i.properties()[0] : null, p != null ? p.name() : null)
					.filter(x -> x != null).findFirst().get();
//			IO.println("n=" + n);
			var k = Stream.of(type.getSimpleName(), n).filter(x -> x != null).collect(Collectors.joining("."));
//			IO.println("k=" + k);
			configuration.indexes.add(k);

			var g = new DefaultIndexKeyGetterFactory().keyGetter(type,
					i.properties().length != 0 ? i.properties() : new String[] { p.name() });
			((DefaultCrud<?, E>) c).indexKeyGetters.put(n, g);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		return new DefaultCrud(type, idConverter(type), this);
	}

	public <ID extends Comparable<ID>> IdHelper<ID> idConverter(Class<?> type) {
		var t = JavaReflect.property(type, "id").type();
		@SuppressWarnings("unchecked")
		var x = (IdHelper<ID>) (t == UUID.class ? new UuidIdHelper() : t == String.class ? new StringIdHelper() : null);
		return x;
	}

	protected void createStoresAndIndexes() {
		database.perform(() -> {
			for (var tc : configuration.cruds.entrySet()) {
				var t = tc.getKey();
				var c = (DefaultCrud<?, ?>) tc.getValue();
				database.createTable(t.getSimpleName(),
						Stream.concat(
								Stream.of(new TableColumn("id",
										Number.class.isAssignableFrom(JavaReflect.property(t, "id").type()) ? "INTEGER"
												: "TEXT",
										true), new TableColumn("content", "TEXT", false)),
								c.indexKeyGetters.keySet().stream().filter(Objects::nonNull)
										.map(x -> new TableColumn(x,
												Number.class.isAssignableFrom(JavaReflect.property(t, x).type())
														? "INTEGER"
														: "TEXT",
												false)))
								.toArray(TableColumn[]::new),
						c.idHelper != null);
			}
			for (var k : configuration.indexes) {
				var ss = k.split("\\.");
				database.createIndex(k, ss[0], ss.length == 2 ? ss[1] : "id");
			}
			return null;
		}, true);
	}
}
