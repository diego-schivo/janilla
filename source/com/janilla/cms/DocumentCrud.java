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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.janilla.persistence.Crud;
import com.janilla.persistence.IdHelper;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class DocumentCrud<ID extends Comparable<ID>, E extends Document<ID>> extends Crud<ID, E> {

	protected final String versionTable;

	public DocumentCrud(Class<E> type, IdHelper<ID> idHelper, Persistence persistence) {
		super(type, idHelper, persistence);
		versionTable = Version.class.getSimpleName() + "<" + type.getSimpleName() + ">";
	}

	@Override
	public E create(E entity) {
//		IO.println("DocumentCrud.create, entity=" + entity);
		return type.isAnnotationPresent(Versions.class) ? persistence.database().perform(() -> {
			var e = super.create(entity);
			var vv = new ArrayList<Version<ID, E>>(1);
			persistence.database().table(versionTable).insert(x -> {
				@SuppressWarnings("unchecked")
				var id = (ID) Long.valueOf(x);
				var v = new Version<>(id, e);
				vv.add(v);
				return new Object[] { x, format(v) };
			});
//			IO.println("DocumentCrud.create, vv=" + vv);
			updateVersionIndexes(null, vv, e.id());
			return e;
		}, true) : super.create(entity);
	}

	public E read(ID id, boolean drafts) {
//		IO.println("DocumentCrud.read, id=" + id + ", drafts=" + drafts);
		if (id == null)
			return null;
		return type.isAnnotationPresent(Versions.class) && drafts ? persistence.database().perform(() -> {
			var e = read(id);
			// IO.println("e=" + e);
			if (e != null) {
				var i = persistence.database().index(versionTable + ".documentId");
				class A {
					ID id;
				}
				var a = new A();
				i.select(new Object[] { id }, x -> {
					var oo = x.findFirst().get();
//					IO.println("oo=" + Arrays.toString(oo));
					@SuppressWarnings("unchecked")
					var id2 = (ID) oo[oo.length - 1];
					a.id = id2;
				});
				var v = readVersion(a.id);
//				IO.println("DocumentCrud.read, v=" + v);
				if (v != null && v.document().updatedAt().isAfter(e.updatedAt()))
					e = v.document();
			}
			return e;
		}, false) : read(id);
	}

	public List<E> read(List<ID> ids, boolean drafts) {
//		IO.println("DocumentCrud.read, ids=" + ids + ", drafts=" + drafts);
		if (ids == null || ids.isEmpty())
			return List.of();
		return type.isAnnotationPresent(Versions.class) && drafts
				? persistence.database().perform(() -> ids.stream().map(x -> read(x, true)).toList(), false)
				: read(ids);
	}

	public E update(ID id, E entity, Set<String> include, boolean newVersion) {
		IO.println("DocumentCrud.update, id=" + id + ", entity=" + entity + ", include=" + include + ", newVersion"
				+ newVersion);
		var exclude = Set.of("id", "createdAt", "updatedAt");
		return type.isAnnotationPresent(Versions.class) ? persistence.database().perform(() -> {
			var e1 = read(id, true);
			E e2;
			class A {
				boolean nv = newVersion;
				ID id;
			}
			var a = new A();
			switch (entity.documentStatus()) {
			case DRAFT:
				e2 = Reflection.copy(Map.of("updatedAt", Instant.now()), Reflection.copy(entity, e1,
						x -> (include == null || include.contains(x)) && !exclude.contains(x)));
				if (e2.documentStatus() != e1.documentStatus())
					a.nv = true;
				break;
			case PUBLISHED:
				e2 = super.update(id, x -> {
					var s1 = x.documentStatus();
					x = Reflection.copy(entity, e1,
							y -> (include == null || include.contains(y)) && !exclude.contains(y));
					if (x.documentStatus() != s1)
						a.nv = true;
					return x;
				});
				break;
			default:
				throw new RuntimeException();
			}
			var vv1 = new ArrayList<Version<ID, E>>(1);
			var vv2 = new ArrayList<Version<ID, E>>(1);
			if (a.nv)
				persistence.database().table(versionTable).insert(x -> {
					@SuppressWarnings("unchecked")
					var id2 = (ID) Long.valueOf(x);
					var v = new Version<>(id2, e2);
					vv2.add(v);
					return new Object[] { x, format(v) };
				});
			else {
				var i = persistence.database().index(versionTable + ".documentId");
				i.select(new Object[] { id }, x -> {
					var oo = x.findFirst().get();
					@SuppressWarnings("unchecked")
					var id2 = (ID) oo[oo.length - 1];
					a.id = id2;
				});
				persistence.database().table(versionTable).delete(new Object[] { a.id }, x -> {
					var oo = x.findFirst().get();
					IO.println("oo=" + Arrays.toString(oo));
					@SuppressWarnings("unchecked")
					var v1 = (Version<ID, E>) parse((String) oo[1], Version.class);
					vv1.add(v1);
				});
				persistence.database().table(versionTable).insert(_ -> {
					var v2 = new Version<>(a.id, e2);
					vv2.add(v2);
					return new Object[] { a.id, format(v2) };
				});
			}
			updateVersionIndexes(vv1, vv2, id);
			return e2;
		}, true)
				: super.update(id, x -> Reflection.copy(entity, x,
						y -> (include == null || include.contains(y)) && !exclude.contains(y)));
	}

	@Override
	public E delete(ID id) {
		return type.isAnnotationPresent(Versions.class) ? persistence.database().perform(() -> {
			var e = super.delete(id);
			var ids = new ArrayList<ID>();
			persistence.database().index(versionTable + ".documentId").select(new Object[] { id }, x -> x.map(oo -> {
				@SuppressWarnings("unchecked")
				var id2 = (ID) oo[1];
				return id2;
			}).forEach(ids::add));
			var vv = new ArrayList<Version<ID, E>>(ids.size());
			for (var id2 : ids)
				persistence.database().table(versionTable).delete(new Object[] { id2 }, x -> {
					@SuppressWarnings("unchecked")
					var v = (Version<ID, E>) parse((String) x.findFirst().get()[1], Version.class);
					vv.add(v);
				});
			updateVersionIndexes(vv, null, id);
			return e;
		}, true) : super.delete(id);
	}

	public List<E> patch(List<ID> ids, E entity, Set<String> include) {
//		if (!type.isAnnotationPresent(Versions.class))
//			throw new UnsupportedOperationException();
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database().perform(() -> ids.stream()
				.map(x -> update(x, entity, include, type.isAnnotationPresent(Versions.class))).toList(), true);
	}

	public List<Version<ID, E>> readVersions(ID id) {
		return persistence.database().perform(() -> {
			var ids = new ArrayList<ID>();
			persistence.database().index(versionTable + ".documentId").select(new Object[] { id }, x -> x.map(oo -> {
				@SuppressWarnings("unchecked")
				var id2 = (ID) oo[1];
				return id2;
			}).forEach(ids::add));
			var vv = new ArrayList<Version<ID, E>>(ids.size());
			for (var id2 : ids)
				persistence.database().table(versionTable).select(new Object[] { id2 }, x -> {
					@SuppressWarnings("unchecked")
					var v = (Version<ID, E>) parse((String) x.findFirst().get()[1], Version.class);
					vv.add(v);
				});
			return vv;
		}, false);
	}

	public Version<ID, E> readVersion(ID versionId) {
		return persistence.database().perform(() -> {
			var vv = new ArrayList<Version<ID, E>>(1);
			persistence.database().table(versionTable).select(new Object[] { versionId }, x -> {
				var oo = x.findFirst().get();
//				IO.println("oo=" + Arrays.toString(oo));
				@SuppressWarnings("unchecked")
				var v = (Version<ID, E>) parse((String) oo[1], Version.class);
				vv.add(v);
			});
			return !vv.isEmpty() ? vv.getFirst() : null;
		}, false);
	}

	public E restoreVersion(ID versionId, DocumentStatus status) {
		return persistence.database().perform(() -> {
			var v1 = readVersion(versionId);
			var e = super.update(v1.document().id(), x -> {
				x = v1.document();
				if (status != x.documentStatus())
					x = Reflection.copy(Map.of("documentStatus", status), x);
				return x;
			});
			var l = persistence.database().table(versionTable).insert(x -> {
				@SuppressWarnings("unchecked")
				var id = (ID) Long.valueOf(x);
				var v = new Version<>(id, e);
				return new Object[] { x, format(v) };
			});
			persistence.database().index(versionTable + ".documentId").insert(new Object[] { e.id(), e.updatedAt(), l },
					null);
			return e;
		}, true);
	}

	protected void updateVersionIndexes(Iterable<Version<ID, E>> vv1, Iterable<Version<ID, E>> vv2, ID id) {
		class A {
			E e1;
			E e2;
		}
		var a = new A();
		var i = persistence.database().index(versionTable + ".documentId");
		if (vv1 != null)
			for (var v : vv1) {
				i.delete(new Object[] { id, v.document().updatedAt().toString(), v.id() }, null);
				if (a.e1 == null)
					a.e1 = v.document();
			}
		if (vv2 != null)
			for (var v : vv2) {
				i.insert(new Object[] { id, v.document().updatedAt().toString(), v.id() }, null);
				if (a.e2 == null)
					a.e2 = v.document();
			}
		updateIndexes(a.e1, a.e2, id,
				x -> persistence.database().index(type.getSimpleName() + "." + x + "Draft", "table"));
	}
}
