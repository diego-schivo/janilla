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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.database.Database;
import com.janilla.io.ElementHelper;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.reflect.Reflection;
import com.janilla.util.Lazy;

public class Persistence {

	protected Class<?>[] types;

	protected Database database;

	protected Supplier<Configuration> configuration = Lazy.of(() -> {
		var c = new Configuration();
		for (var t : types) {
			configure(t, c);
		}
		return c;
	});

	public Class<?>[] getTypes() {
		return types;
	}

	public void setTypes(Class<?>[] types) {
		this.types = types;
	}

	public Database getDatabase() {
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public <K, V> boolean initialize(String name, com.janilla.database.Index<K, V> index) {
		var i = configuration.get().indexInitializers.get(name);
		if (i == null)
			return false;
		@SuppressWarnings("unchecked")
		var j = (IndexInitializer<K, V>) i;
		j.initialize(index);
		return true;
	}

	public <E> Crud<E> getCrud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<E>) configuration.get().cruds.get(class1);
		return c;
	}

	protected <E> Crud<E> newCrud(Class<E> type) {
		return Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())
				|| !type.isAnnotationPresent(Store.class) ? null : new Crud<>();
	}

	<E, K, V> void configure(Class<E> type, Configuration configuration) {
		Crud<E> d = newCrud(type);
		if (d == null)
			return;
		d.type = type;
		d.database = database;
		if (d.formatter == null)
			d.formatter = e -> Json.format(new ReflectionJsonIterator(e));
		if (d.parser == null)
			d.parser = t -> Json.parse((String) t, Json.parseCollector(type));
		configuration.cruds.put(type, d);
		for (var i = Stream.concat(Stream.of(type), Reflection.properties(type).map(n -> {
			try {
				return type.getDeclaredField(n);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		})).iterator();i.hasNext();) {
			var o = i.next();
			var j = o.getAnnotation(Index.class);
			if (j == null)
				continue;

			var n = o instanceof Field f ? f.getName() : null;
			var u = n != null ? Reflection.getter(type, n).getReturnType() : null;
			var b = new IndexInitializer<K, V>();
			if (u == null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.NULL;
				b.keyHelper = h;
			} else if (u == Long.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.LONG;
				b.keyHelper = h;
			} else if (u == String.class || u == Collection.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.STRING;
				b.keyHelper = h;
			} else
				throw new RuntimeException();

			var x = j.sort();
			if (x.startsWith("+") || x.startsWith("-"))
				x = x.substring(1);
			var y = !x.isEmpty() ? Reflection.getter(type, x) : null;
			if (y == null || y.getReturnType() == null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, "id");
				b.valueHelper = h;
			} else {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, j.sort(), "id");
				b.valueHelper = h;
			}
			configuration.indexInitializers.put(type.getSimpleName() + (n != null ? "." + n : ""), b);

			var g = new IndexEntryGetter();
			g.keyGetter = n != null && !n.isEmpty() ? Reflection.getter(type, n) : null;
			g.sortGetter = y;
			d.indexEntryGetters.put(n, g);
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

		Entry<Object, Object> getIndexEntry(Object entity, Long id) throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.invoke(entity) : null;
			var v = sortGetter != null ? new Object[] { sortGetter.invoke(entity), id } : new Object[] { id };
			return new SimpleEntry<>(k, v);
		}
	}
}
