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
package com.janilla.persistence;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.json.TypeResolver;
import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;
import com.janilla.sqlite.SqliteDatabase;
import com.janilla.sqlite.TableColumn;

public class Persistence {

	protected final SqliteDatabase database;

	protected final Collection<Class<? extends Entity<?>>> types;

	protected final TypeResolver typeResolver;

	protected final Configuration configuration = new Configuration();

	public Persistence(SqliteDatabase database, Collection<Class<? extends Entity<?>>> types,
			TypeResolver typeResolver) {
		this.database = database;
		this.types = types;
		this.typeResolver = typeResolver;
		for (var t : types)
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

	public <ID extends Comparable<ID>, E extends Entity<ID>> Crud<ID, E> crud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<ID, E>) configuration.cruds.get(class1);
		return c;
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
			var ae = p != null ? p.annotatedElement() : (AnnotatedElement) o;
			var i = ae.getAnnotation(Index.class);
			if (i == null)
				continue;

//			var t = p != null ? p.genericType() : null;
//			var ii = new IndexFactory<K, V>();
//			ii.keyConverter = keyConverter(t);
//			if (ii.keyConverter == null)
//				throw new NullPointerException("No keyConverter for index on " + p + " (" + t + ")");

//			var s = i.sort();
//			if (s.startsWith("+") || s.startsWith("-"))
//				s = s.substring(1);
//			var sp = !s.isEmpty() ? Reflection.property(type, s) : null;
//			IO.println("Persistence.configure, sp=" + sp);
//			if (sp != null && sp.type() != null) {
//				@SuppressWarnings("unchecked")
//				var h = (ByteConverter<V>) ByteConverter.of(type, i.sort(), "id");
//				ii.valueConverter = h;
//			} else {
//			@SuppressWarnings("unchecked")
//			var h = (ByteConverter<V>) ByteConverter.of(type, "id");
//			ii.valueConverter = h;
//			}
			var k = type.getSimpleName() + (p != null ? "." + p.name() : "");
			configuration.indexFactories.put(k, null);

			var ieg = new IndexEntryGetter();
			BiFunction<Class<?>, String, Function<Object, Object>> kgf;
			try {
				kgf = i.keyGetter().getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			ieg.keyGetter = p != null ? kgf.apply(type, p.name()) : null;
//			ieg.sortGetter = sp;
			c.indexEntryGetters.put(p != null ? p.name() : null, ieg);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		return !Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class) ? new Crud(type, idConverter(type), this) : null;
	}

	public <ID extends Comparable<ID>> IdHelper<ID> idConverter(Class<?> type) {
		var t = Reflection.property(type, "id").type();
		@SuppressWarnings("unchecked")
		var x = (IdHelper<ID>) (t == UUID.class ? new UuidIdHelper()
				: t == String.class ? new StringIdHelper() : null);
		return x;
	}

	protected void createStoresAndIndexes() {
		for (var x : configuration.cruds.entrySet())
			database.perform(() -> {
				database.createTable(x.getKey().getSimpleName(), new TableColumn[0], x.getValue().idHelper != null);
				return null;
			}, true);
		for (var k : configuration.indexFactories.keySet())
			database.perform(() -> {
				database.createIndex(k, "foo");
				return null;
			}, true);
	}

	protected static class Configuration {

		protected Map<Class<?>, Crud<?, ?>> cruds = new HashMap<>();

		protected Map<String, Object> indexFactories = new HashMap<>();

		public Map<Class<?>, Crud<?, ?>> cruds() {
			return cruds;
		}

		public Map<String, Object> indexFactories() {
			return indexFactories;
		}
	}

	protected static class IndexEntryGetter {

		protected Function<Object, Object> keyGetter;

//		protected Property sortGetter;

		protected Map.Entry<Object, Object> getIndexEntry(Object entity, Comparable<?> id)
				throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.apply(entity) : null;
//			var v = sortGetter != null ? new Object[] { sortGetter.get(entity), id } : new Object[] { id };
			var v = id;
			return keyGetter == null || k != null ? new AbstractMap.SimpleImmutableEntry<>(k, v) : null;
		}
	}
}
