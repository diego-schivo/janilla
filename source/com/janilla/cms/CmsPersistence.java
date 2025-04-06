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
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Index;
import com.janilla.database.KeyAndData;
import com.janilla.database.NameAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.persistence.Store;
import com.janilla.reflect.Reflection;

public class CmsPersistence extends Persistence {

	protected static final Crud.Observer DOCUMENT_OBSERVER = new Crud.Observer() {

		@Override
		public <E> E beforeCreate(E entity) {
			var d = (Document) entity;
			var i = Instant.now();
			var v = d.getClass().getAnnotation(Versions.class);
			var m = Map.<String, Object>of("createdAt", i, "updatedAt", i);
			if (d.status() == null) {
				m = new HashMap<>(m);
				m.put("status", v != null && v.drafts() ? Document.Status.DRAFT : Document.Status.PUBLISHED);
			}
			return Reflection.copy(m, entity);
		}

		@Override
		public <E> E beforeUpdate(E entity) {
			var d = (Document) entity;
			var i = Instant.now();
			var m = Map.<String, Object>of("updatedAt", i);
			if (d.status() == Document.Status.PUBLISHED) {
				m = new HashMap<>(m);
				m.put("publishedAt", i);
			}
			return Reflection.copy(m, entity);
		}
	};

	public CmsPersistence(Database database, Iterable<Class<?>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	public <K, V> Index<K, V> newIndex(NameAndData nameAndData) {
		if (nameAndData.name().startsWith(Version.class.getSimpleName() + "<")
				&& nameAndData.name().endsWith(">.document")) {
			@SuppressWarnings("unchecked")
			var i = (Index<K, V>) new Index<Long, Object[]>(
					new BTree<>(database.bTreeOrder(), database.channel(), database.memory(),
							KeyAndData.getByteConverter(ByteConverter.LONG), nameAndData.bTree()),
					ByteConverter.of(Version.class, "-updatedAt", "id"));
			return i;
		}
		return super.newIndex(nameAndData);
	}

	@Override
	protected <E, K, V> void configure(Class<E> type) {
		super.configure(type);
		var v = type.getAnnotation(Versions.class);
		if (v != null && v.drafts())
			for (var k : configuration.indexFactories().keySet().toArray(String[]::new))
				if (k.equals(type.getSimpleName()) || k.startsWith(type.getSimpleName() + "."))
					configuration.indexFactories().put(k + "Draft", configuration.indexFactories().get(k));
	}

	@Override
	protected <E> Crud<E> newCrud(Class<E> type) {
		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class)) {
			@SuppressWarnings("unchecked")
			var t = (Class<? extends Document>) type;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			var c = (Crud<E>) new DocumentCrud(t, this);
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

	protected ByteConverter<Document.Reference<?>> documentReferenceConverter;

	public ByteConverter<Document.Reference<?>> documentReferenceConverter() {
		if (documentReferenceConverter == null)
			documentReferenceConverter = new ByteConverter<>() {

				@Override
				public byte[] serialize(Document.Reference<?> element) {
					var s = element.type().getSimpleName();
					var l = element.id();
					var bb = s.getBytes();
					var b = ByteBuffer.allocate(Integer.BYTES + bb.length + Long.BYTES);
					b.putInt(bb.length);
					b.put(bb);
					b.putLong(l);
					return b.array();
				}

				@Override
				public int getLength(ByteBuffer buffer) {
					var i = buffer.getInt(buffer.position());
					return Integer.BYTES + i + Long.BYTES;
				}

				@Override
				public Document.Reference<?> deserialize(ByteBuffer buffer) {
					var i = buffer.getInt();
					var bb = new byte[i];
					buffer.get(bb);
					var l = buffer.getLong();
					var s = new String(bb);
					var t = typeResolver.apply(new MapAndType(Map.of("$type", s), null)).type();
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var r = new Document.Reference(t, l);
					return r;
				}

				@Override
				public int compare(ByteBuffer buffer, Document.Reference<?> element) {
					var p = buffer.position();
					var i = buffer.getInt(p);
					var bb = new byte[i];
					buffer.get(p + Integer.BYTES, bb);
					var c = new String(bb).compareTo(element.type().getSimpleName());
					return c != 0 ? c : Long.compare(buffer.getLong(p + Integer.BYTES + i), element.id());
				}
			};
		return documentReferenceConverter;
	}

	@Override
	protected <K> ByteConverter<K> keyConverter(Class<?> type) {
		if (type == Document.Reference.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) documentReferenceConverter();
			return h;
		}
		return super.keyConverter(type);
	}
}
