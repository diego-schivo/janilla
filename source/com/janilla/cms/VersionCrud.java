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

public class VersionCrud<E extends Entity> extends Crud<E> {

	protected final String versionStore;

	protected final String versionIndex;

	public VersionCrud(Class<E> type, Persistence persistence) {
		super(type, persistence);
		versionStore = Version.class.getSimpleName() + "<" + type.getSimpleName() + ">";
		versionIndex = versionStore + ".entity";
	}

	@Override
	public E create(E entity) {
		return persistence.database().perform((ss, ii) -> {
			var e = super.create(entity);
			var l = ss.perform(versionStore, s -> s.create(x -> {
				var v = new Version<>(x, e);
				return format(v);
			}));
			ii.perform(versionIndex, i -> {
				i.add(e.id(), new Object[][] { { e.updatedAt(), l } });
				return null;
			});
			return e;
		}, true);
	}

	public E update(long id, E entity, Entity.Status status, boolean version) {
		return persistence.database().perform((ss, ii) -> {
			class A {

				boolean v = version;
			}
			var a = new A();
			var e = super.update(id, x -> {
				x = Reflection.copy(entity, x, y -> !Set.of("id", "createdAt", "updatedAt", "status").contains(y));
				if (status != x.status()) {
					x = Reflection.copy(Map.of("status", status), x);
					a.v = true;
				}
				return x;
			});
			if (a.v) {
				var l = ss.perform(versionStore, s -> s.create(x -> {
					var v = new Version<>(x, e);
					return format(v);
				}));
				ii.perform(versionIndex, i -> {
					i.add(id, new Object[][] { { e.updatedAt(), l } });
					return null;
				});
			} else {
				var oo = ii.perform(versionIndex, i -> (Object[]) i.list(id).findFirst().get());
				var l = (long) oo[1];
				ss.perform(versionStore, s -> s.update(l, _ -> {
					var v = new Version<>(l, e);
					return format(v);
				}));
				ii.perform(versionIndex, i -> {
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
		return persistence.database().perform((ss, ii) -> {
			var e = super.delete(id);
			var ll = ii.perform(versionIndex, i -> {
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
			var ll = ii.perform(versionIndex, i -> i.list(id).mapToLong(x -> (long) ((Object[]) x)[1]).toArray());
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
			ii.perform(versionIndex, i -> {
				i.add(e.id(), new Object[][] { { e.updatedAt(), l } });
				return null;
			});
			return e;
		}, true);
	}

	@Override
	protected E beforeCreate(E entity, long x) {
		var i = Instant.now();
		return Reflection.copy(Map.of("id", x, "createdAt", i, "updatedAt", i, "status", Entity.Status.DRAFT), entity);
	}

	@Override
	protected E beforeUpdate(E entity) {
		return Reflection.copy(Map.of("updatedAt", Instant.now()), entity);
	}
}
