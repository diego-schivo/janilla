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

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.java.Property;
import com.janilla.java.Reflection;
import com.janilla.java.TypeResolver;
import com.janilla.persistence.Entity;
import com.janilla.persistence.Index;

public class Persistence {

	protected final SqliteDatabase database;

	protected final List<Class<? extends Entity<?>>> storables;

	protected final TypeResolver typeResolver;

	protected final PersistenceConfiguration configuration = new PersistenceConfiguration();

	public Persistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, TypeResolver typeResolver) {
		this.database = database;
		this.storables = storables;
		this.typeResolver = typeResolver;
		for (var t : storables)
			configure(t);
		if (database.schema().isEmpty())
			createStoresAndIndexes();
	}

	public SqliteDatabase database() {
		return database;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	@SuppressWarnings("unchecked")
	public <ID extends Comparable<ID>, E extends Entity<ID>> Crud<ID, E> crud(Class<E> type) {
//		IO.println("Persistence.crud, type=" + type);
		return (Crud<ID, E>) Objects.requireNonNull(configuration.cruds.get(type), "type=" + type);
	}

	protected <E extends Entity<?>, K, V> void configure(Class<E> type) {
		Crud<?, E> c = newCrud(type);
		if (c == null)
			return;
//		IO.println("Persistence.configure, type=" + type);
		configuration.cruds.put(type, c);

		for (var oo = Stream.concat(Stream.of(type), Reflection.properties(type)).iterator(); oo.hasNext();) {
			var o = oo.next();
//			IO.println("Persistence.configure, o=" + o);
			var p = o instanceof Property x ? x : null;
			var ae = (AnnotatedElement) (p != null ? p.member() : o);
			var i = ae.getAnnotation(Index.class);
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
			c.indexKeyGetters.put(n, g);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		return new Crud(type, idConverter(type), this);
	}

	public <ID extends Comparable<ID>> IdHelper<ID> idConverter(Class<?> type) {
		var t = Reflection.property(type, "id").type();
		@SuppressWarnings("unchecked")
		var x = (IdHelper<ID>) (t == UUID.class ? new UuidIdHelper() : t == String.class ? new StringIdHelper() : null);
		return x;
	}

	protected void createStoresAndIndexes() {
		database.perform(() -> {
			for (var tc : configuration.cruds.entrySet()) {
				var t = tc.getKey();
				var c = tc.getValue();
				database.createTable(t.getSimpleName(),
						Stream.concat(
								Stream.of(new TableColumn("id",
										Number.class.isAssignableFrom(Reflection.property(t, "id").type()) ? "INTEGER"
												: "TEXT",
										true), new TableColumn("content", "TEXT", false)),
								c.indexKeyGetters.keySet().stream().filter(Objects::nonNull)
										.map(x -> new TableColumn(x,
												Number.class.isAssignableFrom(Reflection.property(t, x).type())
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
