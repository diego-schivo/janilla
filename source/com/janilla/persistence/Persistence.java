/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import com.janilla.database.Database;
import com.janilla.io.ElementHelper;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.reflect.Reflection;

public class Persistence {

	protected Database database;

	protected Configuration configuration;

	public Database getDatabase() {
		return database;
	}

	public <K, V> boolean initializeIndex(String name, com.janilla.database.Index<K, V> index) {
		var i = configuration.indexInitializers.get(name);
		if (i == null)
			return false;
		@SuppressWarnings("unchecked")
		var j = (IndexInitializer<K, V>) i;
		j.initialize(index);
		return true;
	}

	public <E> Crud<E> getCrud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<E>) configuration.cruds.get(class1);
		return c;
	}

	protected void createStoresAndIndexes() throws IOException {
		for (var t : configuration.cruds.keySet())
			database.perform((s, i) -> {
				s.create(t.getSimpleName());
				return null;
			}, true);
		for (var k : configuration.indexInitializers.keySet())
			database.perform((s, i) -> {
				i.create(k);
				return null;
			}, true);
	}

	protected <E> Crud<E> newCrud(Class<E> type) {
		return Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())
				|| !type.isAnnotationPresent(Store.class) ? null : new Crud<>();
	}

	<E, K, V> void configure(Class<E> type) {
		Crud<E> d = newCrud(type);
		if (d == null)
			return;
		d.type = type;
		d.database = database;
		if (d.formatter == null)
			d.formatter = e -> {
				var t = new ReflectionJsonIterator();
				t.setObject(e);
				return Json.format(t);
			};
		if (d.parser == null)
			d.parser = t -> Json.parse((String) t, Json.parseCollector(type));
		configuration.cruds.put(type, d);

		var r = Stream.concat(Stream.of(type), Reflection.properties(type).map(n -> {
			Field f;
			try {
				f = type.getDeclaredField(n);
			} catch (NoSuchFieldException e) {
				f = null;
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			}
			return f;
		}).filter(Objects::nonNull)).iterator();
		while (r.hasNext()) {
			var e = r.next();
			var i = e.getAnnotation(Index.class);
			if (i == null)
				continue;

			var n = e instanceof Field f ? f.getName() : null;
			var t = n != null ? Reflection.getter(type, n).getReturnType() : null;
			var j = new IndexInitializer<K, V>();
			if (t == null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.NULL;
				j.keyHelper = h;
			} else if (t == Long.class || t == Long.TYPE) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.LONG;
				j.keyHelper = h;
			} else if (t == String.class || t == Collection.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.STRING;
				j.keyHelper = h;
			} else if (t == UUID.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.UUID1;
				j.keyHelper = h;
			} else
				throw new RuntimeException();

			var s = i.sort();
			if (s.startsWith("+") || s.startsWith("-"))
				s = s.substring(1);
			var g = !s.isEmpty() ? Reflection.getter(type, s) : null;
			if (g != null && g.getReturnType() != null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, i.sort(), "id");
				j.valueHelper = h;
			} else {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, "id");
				j.valueHelper = h;
			}
			var k = type.getSimpleName() + (n != null ? "." + n : "");
			configuration.indexInitializers.put(k, j);

			var v = new IndexEntryGetter();
			v.keyGetter = n != null && !n.isEmpty() ? Reflection.getter(type, n) : null;
			v.sortGetter = g;
			d.indexEntryGetters.put(n, v);
		}
	}

	static class Configuration {

		Map<String, IndexInitializer<?, ?>> indexInitializers = new HashMap<>();

		Map<Class<?>, Crud<?>> cruds = new HashMap<>();
	}

	static class IndexInitializer<K, V> {

		ElementHelper<K> keyHelper;

		ElementHelper<V> valueHelper;

		void initialize(com.janilla.database.Index<K, V> index) {
			index.setKeyHelper(keyHelper);
			index.setValueHelper(valueHelper);
		}
	}

	static class IndexEntryGetter {

		Method keyGetter;

		Method sortGetter;

		Entry<Object, Object> getIndexEntry(Object entity, long id) throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.invoke(entity) : null;
			var v = sortGetter != null ? new Object[] { sortGetter.invoke(entity), id } : new Object[] { id };
			return keyGetter == null || k != null ? new SimpleEntry<>(k, v) : null;
		}
	}
}
