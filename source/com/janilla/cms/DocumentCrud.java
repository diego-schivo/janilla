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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class DocumentCrud<ID extends Comparable<ID>, E extends Document<ID>> extends Crud<ID, E> {

	protected final String versionStore;

	public DocumentCrud(Class<E> type, Foo<ID> nextId, Persistence persistence) {
		super(type, nextId, persistence);
		versionStore = Version.class.getSimpleName() + "<" + type.getSimpleName() + ">";
	}

	@Override
	public E create(E entity) {
//		IO.println("entity=" + entity);
		if (!type.isAnnotationPresent(Versions.class))
			return super.create(entity);
		return persistence.database().perform(() -> {
			var e = super.create(entity);
			var vv = new ArrayList<Version<ID, E>>(1);
			persistence.database().tableBTree(versionStore).insert(x -> {
				var v = new Version<>((ID) Long.valueOf(x), e);
				vv.add(v);
				return new Object[] { format(v) };
			});
			updateVersionIndexes(null, vv, e.id());
			return e;
		}, true);
	}

	public E read(ID id, boolean drafts) {
		if (id == null)
			return null;
		if (!type.isAnnotationPresent(Versions.class) || !drafts)
			return read(id);
		return persistence.database().perform(() -> {
			var e = read(id);
			// IO.println("e=" + e);
			if (e != null) {
				var i = persistence.database().indexBTree(versionStore + ".document");
				@SuppressWarnings("unchecked")
				var l = (ID) ((Object[]) i.select(id).findFirst().get())[1];
				var v = readVersion(l);
				// IO.println("v=" + v);
				if (v.document().updatedAt().isAfter(e.updatedAt()))
					e = v.document();
			}
			return e;
		}, false);
	}

	public List<E> read(List<ID> ids, boolean drafts) {
		if (ids == null || ids.isEmpty())
			return List.of();
		if (!type.isAnnotationPresent(Versions.class) || !drafts)
			return read(ids);
		return persistence.database().perform(() -> ids.stream().map(x -> read(x, true)).toList(), false);
	}

	public E update(ID id, E entity, Set<String> include, boolean newVersion) {
		var exclude = Set.of("id", "createdAt", "updatedAt");
		if (!type.isAnnotationPresent(Versions.class))
			return super.update(id, x -> Reflection.copy(entity, x,
					y -> (include == null || include.contains(y)) && !exclude.contains(y)));
//		return persistence.database().perform((ss, ii) -> {
//			var e1 = read(id, true);
//			E e2;
//			class A {
//
//				boolean nv = newVersion;
//			}
//			var a = new A();
//			switch (entity.documentStatus()) {
//			case DRAFT:
//				e2 = Reflection.copy(Map.of("updatedAt", Instant.now()), Reflection.copy(entity, e1,
//						y -> (include == null || include.contains(y)) && !exclude.contains(y)));
//				if (e2.documentStatus() != e1.documentStatus())
//					a.nv = true;
//				break;
//			case PUBLISHED:
//				e2 = super.update(id, x -> {
//					var s1 = x.documentStatus();
//					x = Reflection.copy(entity, e1,
//							y -> (include == null || include.contains(y)) && !exclude.contains(y));
//					if (x.documentStatus() != s1)
//						a.nv = true;
//					return x;
//				});
//				break;
//			default:
//				throw new RuntimeException();
//			}
//			var vv1 = new ArrayList<Version<ID, E>>(1);
//			var vv2 = new ArrayList<Version<ID, E>>(1);
//			if (a.nv)
//				ss.perform(versionStore, s0 -> {
//					@SuppressWarnings("unchecked")
//					var s = (Store<ID, String>) s0;
//					var id2 = nextId.apply(s.getAttributes());
//					var v = new Version<>(id2, e2);
//					vv2.add(v);
//					s.create(id2, format(v));
//					return null;
//				});
//			else {
//				@SuppressWarnings("unchecked")
//				var l = (ID) ii.perform(versionStore + ".document", i -> (Object[]) i.list(id).findFirst().get())[1];
//				ss.perform(versionStore, s0 -> {
//					@SuppressWarnings("unchecked")
//					var s = (Store<ID, String>) s0;
//					s.update(l, x -> {
//						@SuppressWarnings("unchecked")
//						var v1 = (Version<ID, E>) parse((String) x, Version.class);
//						vv1.add(v1);
//						var v2 = new Version<>(v1.id(), e2);
//						vv2.add(v2);
//						return format(v2);
//					});
//					return null;
//				});
//			}
//			updateVersionIndexes(vv1, vv2, id);
//			return e2;
//		}, true);
		throw new RuntimeException();
	}

	@Override
	public E delete(ID id) {
		if (!type.isAnnotationPresent(Versions.class))
			return super.delete(id);
//		return persistence.database().perform((ss, ii) -> {
//			var e = super.delete(id);
//			var ll = ii.perform(versionStore + ".document", i -> i.list(id).map(x -> {
//				@SuppressWarnings("unchecked")
//				var y = (ID) ((Object[]) x)[1];
//				return y;
//			}).toList());
//			var vv = ss.perform(versionStore, s0 -> {
//				@SuppressWarnings("unchecked")
//				var s = (Store<ID, String>) s0;
//				return ll.stream().map(x -> {
//					var o = s.delete(x);
//					@SuppressWarnings("unchecked")
//					var v = (Version<ID, E>) parse((String) o, Version.class);
//					return v;
//				});
//			}).toList();
//			updateVersionIndexes(vv, null, id);
//			return e;
//		}, true);
		throw new RuntimeException();
	}

	public List<E> patch(List<ID> ids, E entity, Set<String> include) {
//		if (!type.isAnnotationPresent(Versions.class))
//			throw new UnsupportedOperationException();
		if (ids == null || ids.isEmpty())
			return List.of();
//		return persistence.database().perform((_, _) -> ids.stream()
//				.map(x -> update(x, entity, include, type.isAnnotationPresent(Versions.class))).toList(), true);
		throw new RuntimeException();
	}

	public List<Version<ID, E>> readVersions(ID id) {
//		return persistence.database().perform((ss, ii) -> {
//			var ll = ii.perform(versionStore + ".document", i -> i.list(id).map(x -> {
//				@SuppressWarnings("unchecked")
//				ID y = (ID) ((Object[]) x)[1];
//				return y;
//			}).toList());
//			var vv = ss.perform(versionStore, s0 -> {
//				@SuppressWarnings("unchecked")
//				var s = (Store<ID, String>) s0;
//				return ll.stream().map(x -> {
//					var o = s.read(x);
//					@SuppressWarnings("unchecked")
//					var v = (Version<ID, E>) parse((String) o, Version.class);
//					return v;
//				});
//			}).toList();
//			return vv;
//		}, false);
		throw new RuntimeException();
	}

	public Version<ID, E> readVersion(ID versionId) {
//		return persistence.database().perform((ss, _) -> ss.perform(versionStore, s0 -> {
//			@SuppressWarnings("unchecked")
//			var s = (Store<ID, String>) s0;
//			var o = s.read(versionId);
//			@SuppressWarnings("unchecked")
//			var v = (Version<ID, E>) parse((String) o, Version.class);
//			return v;
//		}), false);
		throw new RuntimeException();
	}

	public E restoreVersion(ID versionId, Document.Status status) {
//		return persistence.database().perform((ss, ii) -> {
//			var v1 = ss.perform(versionStore, s0 -> {
//				@SuppressWarnings("unchecked")
//				var s = (Store<ID, String>) s0;
//				var o = s.read(versionId);
//				@SuppressWarnings("unchecked")
//				var v = (Version<ID, E>) parse((String) o, Version.class);
//				return v;
//			});
//			var e = super.update(v1.document().id(), x -> {
//				x = v1.document();
//				if (status != x.documentStatus())
//					x = Reflection.copy(Map.of("documentStatus", status), x);
//				return x;
//			});
//			var l = ss.perform(versionStore, s0 -> {
//				@SuppressWarnings("unchecked")
//				var s = (Store<ID, String>) s0;
//				var id = nextId.apply(s.getAttributes());
//				var v2 = new Version<>(id, e);
//				s.create(id, format(v2));
//				return null;
//			});
//			ii.perform(versionStore + ".document", i -> {
//				i.add(e.id(), new Object[][] { { e.updatedAt(), l } });
//				return null;
//			});
//			return e;
//		}, true);
		throw new RuntimeException();
	}

	protected void updateVersionIndexes(Iterable<Version<ID, E>> vv1, Iterable<Version<ID, E>> vv2, ID id) {
		class A {

			E e1;

			E e2;
		}
		var a = new A();
		persistence.database().perform(() -> {
			var i = persistence.database().indexBTree(versionStore + ".document");
			if (vv1 != null)
				for (var v : vv1) {
					i.delete(id, v.document().updatedAt().toString(), v.id());
					if (a.e1 == null)
						a.e1 = v.document();
				}
			if (vv2 != null)
				for (var v : vv2) {
					i.insert(id, v.document().updatedAt().toString(), v.id());
					if (a.e2 == null)
						a.e2 = v.document();
				}
			return null;
		}, true);
		updateIndexes(a.e1, a.e2, id, x -> getIndexName(x) + "Draft");
	}
}
