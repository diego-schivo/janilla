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
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.database.Database;
import com.janilla.io.ElementHelper;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;

public class Persistence {

	protected Database database;

	protected Configuration configuration;

	Function<String, Class<?>> typeResolver;

	public Database database() {
		return database;
	}

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
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

	public <E> Crud<E> crud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<E>) configuration.cruds.get(class1);
		return c;
	}

	protected void createStoresAndIndexes() {
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

	protected <E> Crud<E> createCrud(Class<E> type) {
		return Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())
				|| !type.isAnnotationPresent(Store.class) ? null : new Crud<>();
	}

	<E, K, V> void configure(Class<E> type) {
		Crud<E> c = createCrud(type);
		if (c == null)
			return;
		c.type = type;
		c.database = database;
		if (c.formatter == null)
			c.formatter = e -> {
				var t = new ReflectionJsonIterator();
				t.setObject(e);
				t.setIncludeType(true);
				return Json.format(t);
			};
		if (c.parser == null)
			c.parser = t -> {
				var z = new Converter();
				z.setResolver(x -> x.map().containsKey("$type")
						? new Converter.MapType(x.map(), typeResolver.apply((String) x.map().get("$type")))
						: null);
				@SuppressWarnings("unchecked")
				var e = (E) z.convert(Json.parse((String) t), type);
				return e;
			};
		c.indexPresent = type.isAnnotationPresent(Index.class);
		configuration.cruds.put(type, c);

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
			var t = n != null ? Reflection.property(type, n).getType() : null;
			var j = new IndexInitializer<K, V>();
			if (t == null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.NULL;
				j.keyHelper = h;
			} else if (t == Boolean.class || t == Boolean.TYPE || t == boolean[].class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.BOOLEAN;
				j.keyHelper = h;
			} else if (t == Integer.class || t == Integer.TYPE || t == int[].class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.INTEGER;
				j.keyHelper = h;
			} else if (t == Long.class || t == Long.TYPE || t == long[].class) {
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
			var g = !s.isEmpty() ? Reflection.property(type, s) : null;
			if (g != null && g.getType() != null) {
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
			BiFunction<Class<?>, String, Function<Object, Object>> f;
			try {
				f = i.keyGetter().getConstructor().newInstance();
			} catch (ReflectiveOperationException x) {
				throw new RuntimeException(x);
			}
			v.keyGetter = n != null && !n.isEmpty() ? f.apply(type, n) : null;
			v.sortGetter = g;
			c.indexEntryGetters.put(n, v);
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

		Function<Object, Object> keyGetter;

		Property sortGetter;

		Map.Entry<Object, Object> getIndexEntry(Object entity, long id) throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.apply(entity) : null;
			var v = sortGetter != null ? new Object[] { sortGetter.get(entity), id } : new Object[] { id };
			return keyGetter == null || k != null ? new AbstractMap.SimpleEntry<>(k, v) : null;
		}
	}
}
