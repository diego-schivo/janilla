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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Index;
import com.janilla.database.KeyAndData;
import com.janilla.io.ByteConverter;
import com.janilla.io.ByteConverter.SortOrder;
import com.janilla.io.ByteConverter.TypeAndOrder;
import com.janilla.json.MapAndType;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Entity;
import com.janilla.persistence.Persistence;
import com.janilla.persistence.Store;
import com.janilla.reflect.Reflection;

public class CmsPersistence extends Persistence {

	protected static final Crud.Observer DOCUMENT_OBSERVER = new Crud.Observer() {

		@Override
		public <E> E beforeCreate(E entity) {
			var d = (Document<?>) entity;
			var i = Instant.now();
			var v = d.getClass().getAnnotation(Versions.class);
			var m = Map.<String, Object>of("createdAt", i, "updatedAt", i);
			if (d.documentStatus() == null) {
				m = new HashMap<>(m);
				m.put("documentStatus", v != null && v.drafts() ? Document.Status.DRAFT : Document.Status.PUBLISHED);
			}
			return Reflection.copy(m, entity);
		}

		@Override
		public <E> E beforeUpdate(E entity) {
			var d = (Document<?>) entity;
			var i = Instant.now();
			var m = Map.<String, Object>of("updatedAt", i);
			if (d.documentStatus() == Document.Status.PUBLISHED) {
				m = new HashMap<>(m);
				m.put("publishedAt", i);
			}
			return Reflection.copy(m, entity);
		}
	};

	public CmsPersistence(Database database, Set<Class<? extends Entity<?>>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	public <K, V> Index<K, V> newIndex(KeyAndData<String> keyAndData) {
		if (keyAndData.key().startsWith(Version.class.getSimpleName() + "<")
				&& keyAndData.key().endsWith(">.document")) {
			@SuppressWarnings("unchecked")
			var i = (Index<K, V>) new Index<Long, Object[]>(new BTree<>(database.bTreeOrder(), database.channel(),
					database.memory(), KeyAndData.getByteConverter(ByteConverter.LONG), keyAndData.bTree()),
//					ByteConverter.of(Version.class, "-updatedAt", "id"));
					ByteConverter.of(new TypeAndOrder(Instant.class, SortOrder.DESCENDING),
							new TypeAndOrder(Long.class, SortOrder.ASCENDING)));
			return i;
		}
		return super.newIndex(keyAndData);
	}

	@Override
	protected <E extends Entity<?>, K, V> void configure(Class<E> type) {
		super.configure(type);
		var v = type.getAnnotation(Versions.class);
		if (v != null && v.drafts())
			for (var k : configuration.indexFactories().keySet().toArray(String[]::new))
				if (k.equals(type.getSimpleName()) || k.startsWith(type.getSimpleName() + "."))
					configuration.indexFactories().put(k + "Draft", configuration.indexFactories().get(k));
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class)) {
			@SuppressWarnings("unchecked")
			var t = (Class<? extends Document<?>>) type;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var c = (Crud<?, E>) new DocumentCrud(t, nextId(t), this);
			c.observers().add(DOCUMENT_OBSERVER);
			return c;
		}
		return null;
	}

	@Override
	protected void createStoresAndIndexes() {
		super.createStoresAndIndexes();
		for (var t : configuration.cruds().keySet())
			if (t.isAnnotationPresent(Versions.class))
				database.perform((ss, ii) -> {
					var n = Version.class.getSimpleName() + "<" + t.getSimpleName() + ">";
					ss.create(n);
					ii.create(n + ".document");
					return null;
				}, true);
	}

//	protected ByteConverter<Document.Reference<ID, ?>> documentReferenceConverter;

	public <ID extends Comparable<ID>> ByteConverter<Document.Reference<ID, ?>> documentReferenceConverter(
			ByteConverter<ID> idByteConverter) {
//		if (documentReferenceConverter == null)
//			documentReferenceConverter = 
		return new ByteConverter<>() {

			@Override
			public byte[] serialize(Document.Reference<ID, ?> element) {
				var bb1 = element.type().getSimpleName().getBytes();
				var bb2 = idByteConverter.serialize(element.id());
				var b = ByteBuffer.allocate(Integer.BYTES + bb1.length + bb2.length);
				b.putInt(bb1.length);
				b.put(bb1);
				b.put(bb2);
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				var p = buffer.position();
				var i = buffer.getInt(p);
				var l1 = Integer.BYTES + i;
				buffer.position(p + l1);
				try {
					var l2 = idByteConverter.getLength(buffer);
					return l1 + l2;
				} finally {
					buffer.position(p);
				}
			}

			@Override
			public Document.Reference<ID, ?> deserialize(ByteBuffer buffer) {
				var i = buffer.getInt();
				var bb = new byte[i];
				buffer.get(bb);
				var l = idByteConverter.deserialize(buffer);
				var s = new String(bb);
				var t = typeResolver.apply(new MapAndType(Map.of("$type", s), null)).type();
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var r = (Document.Reference<ID, ?>) new Document.Reference(t, l);
				return r;
			}

			@Override
			public int compare(ByteBuffer buffer, Document.Reference<ID, ?> element) {
				var p = buffer.position();
				try {
					var bb = new byte[buffer.getInt()];
					buffer.get(bb);
					var c = new String(bb).compareTo(element.type().getSimpleName());
					return c != 0 ? c : idByteConverter.compare(buffer, element.id());
				} finally {
					buffer.position(p);
				}
			}
		};
//		return documentReferenceConverter;
	}

	@Override
	protected <K> ByteConverter<K> keyConverter(Type type) {
		if (type instanceof ParameterizedType x && x.getRawType() == Document.Reference.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) documentReferenceConverter(ByteConverter.LONG);
			return h;
		}
		return super.keyConverter(type);
	}
}
