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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class DocumentCrud<E extends Document> extends Crud<E> {

	protected final boolean versions;

	protected final String versionStore;

	public DocumentCrud(Class<E> type, Persistence persistence, boolean versions) {
		super(type, persistence);
		this.versions = versions;
		versionStore = Version.class.getSimpleName() + "<" + type.getSimpleName() + ">";
	}

	@Override
	public E create(E entity) {
		if (!versions)
			return super.create(entity);
		return persistence.database().perform((ss, _) -> {
			var e = super.create(entity);
			var vv = new ArrayList<Version<E>>(1);
			ss.perform(versionStore, s -> s.create(x -> {
				var v = new Version<>(x, e);
				vv.add(v);
				return format(v);
			}));
			updateVersionIndexes(null, vv, e.id());
			return e;
		}, true);
	}

	public E read(long id, boolean drafts) {
		if (!versions || !drafts)
			return read(id);
		if (id <= 0)
			return null;
		return persistence.database().perform((_, ii) -> {
			var e = read(id);
//			System.out.println("e=" + e);
			var l = (long) ii.perform(versionStore + ".document", i -> ((Object[]) i.list(id).findFirst().get())[1]);
			var v = readVersion(l);
//			System.out.println("v=" + v);
			if (v.document().updatedAt().isAfter(e.updatedAt()))
				e = v.document();
			return e;
		}, false);
	}

	public Stream<E> read(long[] ids, boolean drafts) {
		if (!versions || !drafts)
			return read(ids);
		if (ids == null || ids.length == 0)
			return Stream.empty();
		return Arrays.stream(ids).mapToObj(x -> read(x, true));
	}

	public E update(long id, E entity, Document.Status status, boolean newVersion) {
		var z = Set.of("id", "createdAt", "updatedAt", "status", "publishedAt");
		if (!versions)
			return super.update(id, x -> Reflection.copy(entity, x, y -> !z.contains(y)));
		return persistence.database().perform((ss, ii) -> {
			E e2;
			class A {

				boolean nv = newVersion;
			}
			var a = new A();
			switch (status) {
			case DRAFT:
				var e1 = super.read(id);
				e2 = Reflection.copy(Map.of("updatedAt", Instant.now()),
						Reflection.copy(entity, e1, y -> !z.contains(y)));
				if (status != e1.status())
					a.nv = true;
				break;
			case PUBLISHED:
				e2 = super.update(id, x -> {
					x = Reflection.copy(entity, x, y -> !z.contains(y));
					if (status != x.status()) {
						x = Reflection.copy(Map.of("status", status), x);
						a.nv = true;
					}
					return x;
				});
				break;
			default:
				throw new RuntimeException();
			}
			var vv1 = new ArrayList<Version<E>>(1);
			var vv2 = new ArrayList<Version<E>>(1);
			if (a.nv)
				ss.perform(versionStore, s -> s.create(x -> {
					var v = new Version<>(x, e2);
					vv2.add(v);
					return format(v);
				}));
			else {
				var l = (long) ii.perform(versionStore + ".document", i -> (Object[]) i.list(id).findFirst().get())[1];
				ss.perform(versionStore, s -> s.update(l, x -> {
					@SuppressWarnings("unchecked")
					var v1 = (Version<E>) parse((String) x, Version.class);
					vv1.add(v1);
					var v2 = new Version<>(v1.id(), e2);
					vv2.add(v2);
					return format(v2);
				}));
			}
			updateVersionIndexes(vv1, vv2, id);
			return e2;
		}, true);
	}

	@Override
	public E delete(long id) {
		if (!versions)
			return super.delete(id);
		return persistence.database().perform((ss, ii) -> {
			var e = super.delete(id);
			var ll = ii.perform(versionStore + ".document",
					i -> i.list(id).mapToLong(x -> (long) ((Object[]) x)[1]).toArray());
			var vv = ss.perform(versionStore, s -> Arrays.stream(ll).mapToObj(x -> {
				var o = s.delete(x);
				@SuppressWarnings("unchecked")
				var v = (Version<E>) parse((String) o, Version.class);
				return v;
			})).toList();
			updateVersionIndexes(vv, null, id);
			return e;
		}, true);
	}

	public Stream<Version<E>> readVersions(long id) {
		return persistence.database().perform((ss, ii) -> {
			var ll = ii.perform(versionStore + ".document",
					i -> i.list(id).mapToLong(x -> (long) ((Object[]) x)[1]).toArray());
			var vv = ss.perform(versionStore, s -> Arrays.stream(ll).mapToObj(x -> {
				var o = s.read(x);
				@SuppressWarnings("unchecked")
				var v = (Version<E>) parse((String) o, Version.class);
				return v;
			}));
			return vv;
		}, false);
	}

	public Version<E> readVersion(long versionId) {
		return persistence.database().perform((ss, _) -> ss.perform(versionStore, s -> {
			var o = s.read(versionId);
			@SuppressWarnings("unchecked")
			var v = (Version<E>) parse((String) o, Version.class);
			return v;
		}), false);
	}

	public E restoreVersion(long versionId, Document.Status status) {
		return persistence.database().perform((ss, ii) -> {
			var v1 = ss.perform(versionStore, s -> {
				var o = s.read(versionId);
				@SuppressWarnings("unchecked")
				var v = (Version<E>) parse((String) o, Version.class);
				return v;
			});
			var e = super.update(v1.document().id(), x -> {
				x = v1.document();
				if (status != x.status())
					x = Reflection.copy(Map.of("status", status), x);
				return x;
			});
			var l = ss.perform(versionStore, s -> s.create(x -> {
				var v2 = new Version<>(x, e);
				return format(v2);
			}));
			ii.perform(versionStore + ".document", i -> {
				i.add(e.id(), new Object[][] { { e.updatedAt(), l } });
				return null;
			});
			return e;
		}, true);
	}

	@Override
	protected E beforeCreate(E entity, long x) {
		var i = Instant.now();
		var v = entity.getClass().getAnnotation(Versions.class);
		var m = Map.of("id", x, "createdAt", i, "updatedAt", i);
		if (entity.status() == null) {
			m = new HashMap<>(m);
			m.put("status", v != null && v.drafts() ? Document.Status.DRAFT : Document.Status.PUBLISHED);
		}
		return Reflection.copy(m, entity);
	}

	@Override
	protected E beforeUpdate(E entity) {
		var i = Instant.now();
		Map<String, Instant> m = Map.of("updatedAt", i);
		if (entity.status() == Document.Status.PUBLISHED) {
			m = new HashMap<>(m);
			m.put("publishedAt", i);
		}
		return Reflection.copy(m, entity);
	}

	protected void updateVersionIndexes(Iterable<Version<E>> vv1, Iterable<Version<E>> vv2, long id) {
		class A {

			E e1;

			E e2;
		}
		var a = new A();
		persistence.database().perform((_, ii) -> ii.perform(versionStore + ".document", i -> {
			if (vv1 != null)
				for (var v : vv1) {
					i.remove(id, new Object[][] { { v.document().updatedAt(), v.id() } });
					if (a.e1 == null)
						a.e1 = v.document();
				}
			if (vv2 != null)
				for (var v : vv2) {
					i.add(id, new Object[][] { { v.document().updatedAt(), v.id() } });
					if (a.e2 == null)
						a.e2 = v.document();
				}
			return null;
		}), true);
		updateIndexes(a.e1, a.e2, id, x -> getIndexName(x) + "Draft");
	}
}
