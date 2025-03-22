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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class EntityCrud<E extends Entity> extends Crud<E> {

	protected final boolean versions;

	protected final String versionStore;

	public EntityCrud(Class<E> type, Persistence persistence, boolean versions) {
		super(type, persistence);
		this.versions = versions;
		versionStore = Version.class.getSimpleName() + "<" + type.getSimpleName() + ">";
	}

	@Override
	public E create(E entity) {
		if (!versions)
			return super.create(entity);
		return persistence.database().perform((ss, ii) -> {
			var e = super.create(entity);
			var l = ss.perform(versionStore, s -> s.create(x -> {
				var v = new Version<>(x, e);
				return format(v);
			}));
			ii.perform(versionStore + ".entity", i -> {
				i.add(e.id(), new Object[][] { { e.updatedAt(), l } });
				return null;
			});
			return e;
		}, true);
	}

	public E update(long id, E entity, Entity.Status status, boolean newVersion) {
		if (!versions)
			return super.update(id, x -> Reflection.copy(entity, x,
					y -> !Set.of("id", "createdAt", "updatedAt", "status").contains(y)));
		return persistence.database().perform((ss, ii) -> {
			class A {

				boolean nv = newVersion;
			}
			var a = new A();
			var e = super.update(id, x -> {
				x = Reflection.copy(entity, x, y -> !Set.of("id", "createdAt", "updatedAt", "status").contains(y));
				if (status != x.status()) {
					x = Reflection.copy(Map.of("status", status), x);
					a.nv = true;
				}
				return x;
			});
			if (a.nv) {
				var l = ss.perform(versionStore, s -> s.create(x -> {
					var v = new Version<>(x, e);
					return format(v);
				}));
				ii.perform(versionStore + ".entity", i -> {
					i.add(id, new Object[][] { { e.updatedAt(), l } });
					return null;
				});
			} else {
				var oo = ii.perform(versionStore + ".entity", i -> (Object[]) i.list(id).findFirst().get());
				var l = (long) oo[1];
				ss.perform(versionStore, s -> s.update(l, _ -> {
					var v = new Version<>(l, e);
					return format(v);
				}));
				ii.perform(versionStore + ".entity", i -> {
					i.remove(id, new Object[][] { oo });
					i.add(id, new Object[][] { { e.updatedAt(), l } });
					return null;
				});
			}
			return e;
		}, true);
	}

	@Override
	public E delete(long id) {
		if (!versions)
			return super.delete(id);
		return persistence.database().perform((ss, ii) -> {
			var e = super.delete(id);
			var ll = ii.perform(versionStore + ".entity", i -> {
				var oo = i.list(id).toArray();
				i.remove(id, (Object[]) oo);
				return oo;
			});
			ss.perform(versionStore, s -> {
				for (var l : ll)
					s.delete((long) l);
				return null;
			});
			return e;
		}, true);
	}

	public Stream<Version<E>> readVersions(long id) {
		return persistence.database().perform((ss, ii) -> {
			var ll = ii.perform(versionStore + ".entity",
					i -> i.list(id).mapToLong(x -> (long) ((Object[]) x)[1]).toArray());
			var c = new Converter(persistence.typeResolver());
			var vv = ss.perform(versionStore, s -> Arrays.stream(ll).mapToObj(x -> {
				var o = s.read(x);
				var o2 = Json.parse((String) o);
				@SuppressWarnings("unchecked")
				var v = (Version<E>) c.convert(o2, Version.class);
				return v;
			}));
			return vv;
		}, false);
	}

	public Version<E> readVersion(long versionId) {
		return persistence.database().perform((ss, _) -> ss.perform(versionStore, s -> {
			var o = s.read(versionId);
			var o2 = Json.parse((String) o);
			var c = new Converter(persistence.typeResolver());
			@SuppressWarnings("unchecked")
			var v = (Version<E>) c.convert(o2, Version.class);
			return v;
		}), false);
	}

	public E restoreVersion(long versionId, Entity.Status status) {
		return persistence.database().perform((ss, ii) -> {
			var v1 = ss.perform(versionStore, s -> {
				var o = s.read(versionId);
				var s2 = Json.parse((String) o);
				@SuppressWarnings("unchecked")
				var v = (Version<E>) new Converter(persistence.typeResolver()).convert(s2, Version.class);
				return v;
			});
			var e = super.update(v1.entity().id(), x -> {
				x = v1.entity();
				if (status != x.status())
					x = Reflection.copy(Map.of("status", status), x);
				return x;
			});
			var l = ss.perform(versionStore, s -> s.create(x -> {
				var v2 = new Version<>(x, e);
				return format(v2);
			}));
			ii.perform(versionStore + ".entity", i -> {
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
		return Reflection.copy(Map.of("id", x, "createdAt", i, "updatedAt", i, "status",
				v != null && v.drafts() ? Entity.Status.DRAFT : Entity.Status.PUBLISHED), entity);
	}

	@Override
	protected E beforeUpdate(E entity) {
		return Reflection.copy(Map.of("updatedAt", Instant.now()), entity);
	}
}
