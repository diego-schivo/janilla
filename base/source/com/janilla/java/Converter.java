/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.java;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Converter {

	protected final TypeResolver typeResolver;

	public Converter() {
		this(null);
	}

	public Converter(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object object, Type target) {
//		IO.println("Converter.convert, object=" + object + ", target=" + target);
//		IO.println(Json.format(object));
		var c = Java.toClass(target);

		if (object == null || (object instanceof String x && x.isEmpty())) {
			if (c == Boolean.TYPE)
				return (T) Boolean.FALSE;
			if (c == Double.TYPE)
				return (T) Double.valueOf(0);
			if (c == Integer.TYPE)
				return (T) Integer.valueOf(0);
			if (c == Long.TYPE)
				return (T) Long.valueOf(0);
			if (c == String.class)
				return (T) object;
			return null;
		}

		if (c != null && c.isAssignableFrom(object.getClass()))
			if (c != Object.class && !c.isArray() && !Collection.class.isAssignableFrom(c)
					&& !Map.class.isAssignableFrom(c))
				return (T) object;

		if (c == BigDecimal.class)
			return (T) switch (object) {
			case Double x -> BigDecimal.valueOf(x);
			case Integer x -> BigDecimal.valueOf(x);
			case Long x -> BigDecimal.valueOf(x);
			case String x -> new BigDecimal(x);
			default -> throw new IllegalArgumentException();
			};

		if (c == Boolean.class || c == Boolean.TYPE)
			return (T) switch (object) {
			case Boolean _ -> object;
			case String x -> Boolean.parseBoolean(x);
			default -> throw new IllegalArgumentException();
			};

		if (c == Instant.class)
			return (T) Instant.parse((String) object);

		if (c == Integer.class || c == Integer.TYPE)
			return (T) switch (object) {
			case Integer _ -> object;
			case Long x -> x.intValue();
			case String x -> Integer.parseInt(x);
			default -> throw new IllegalArgumentException();
			};

		if (c == LocalDate.class)
			return (T) LocalDate.parse((String) object);

		if (c == Locale.class)
			return (T) Locale.forLanguageTag((String) object);

		if (c == Long.class || c == Long.TYPE)
			return (T) switch (object) {
			case Integer x -> x.longValue();
			case Long _ -> object;
			case String x -> Long.parseLong(x);
			default -> throw new IllegalArgumentException();
			};

		if (c == OffsetDateTime.class)
			return (T) OffsetDateTime.parse((String) object);

		if (c == Path.class)
			return (T) Path.of((String) object);

		if (c == URI.class)
			return (T) URI.create((String) object);

		if (c == UUID.class)
			return (T) UUID.fromString((String) object);

		if (c == byte[].class)
			return (T) Base64.getDecoder().decode((String) object);

		if (c == Class.class)
			return (T) typeResolver.parse((String) object);

		if (c != null && c.isEnum()) {
			var n = switch (object) {
			case String x -> x;
			case Map<?, ?> x -> (String) x.get("name");
			default -> throw new IllegalArgumentException();
			};
			@SuppressWarnings("rawtypes")
			var x = Enum.valueOf((Class) c, n);
			return (T) x;
		}

		if (c != null && (c.isArray() || Collection.class.isAssignableFrom(c))) {
			var oo = switch (object) {
			case Object[] x -> Arrays.stream(x);
			case Collection<?> x -> x.stream();
			default -> throw new IllegalArgumentException();
			};
			var t = c.isArray() ? c.componentType() : ((ParameterizedType) target).getActualTypeArguments()[0];
			oo = oo.map(x -> convert(x, t));

			if (c.isArray()) {
				if (c.componentType() == Double.TYPE)
					return (T) oo.mapToDouble(x -> (double) x).toArray();
				if (c.componentType() == Integer.TYPE)
					return (T) oo.mapToInt(x -> (int) x).toArray();
				if (c.componentType() == Long.TYPE)
					return (T) oo.mapToLong(x -> (long) x).toArray();
				return (T) oo.toArray(x -> (Object[]) Array.newInstance(Java.toClass(t), x));
			}
			if (c == List.class)
				return (T) oo.toList();
			if (c == Set.class)
				return (T) oo.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		if (object instanceof Map<?, ?> m) {
			if (c == Map.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				return (T) m.entrySet().stream().map(x -> {
					var k = convert(x.getKey(), aa[0]);
					var v = convert(x.getValue(), aa[1]);
					return Java.mapEntry(k, v);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, x) -> x, LinkedHashMap::new));
			}
			if (c == Map.Entry.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				var k = convert(m.get("key"), aa[0]);
				var v = convert(m.get("value"), aa[1]);
				return (T) Java.mapEntry(k, v);
			}
			return (T) convertMap(m, target, typeResolver);
		}

		return (T) object;
	}

	protected Object convertMap(Map<?, ?> map, Type target, TypeResolver typeResolver) {
//		IO.println("Converter.convertMap, map=" + map + ", target=" + target);
//		IO.println(Json.format(map));
		var c = target != null ? Java.toClass(target) : null;
		var td = typeResolver != null ? typeResolver.apply(new TypedData(map, target)) : null;
//		IO.println("td=" + td);
		if (td != null) {
			if (c != null && !c.isAssignableFrom(Java.toClass(td.type())))
				throw new RuntimeException("c=" + c + ", td.type()=" + td.type());
			map = (Map<?, ?>) td.data();
			target = td.type();
			c = Java.toClass(target);
		}

		Object o;
		if (c.isEnum()) {
			var n = (String) map.get("name");
			if (n != null && !n.isEmpty()) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var x = Enum.valueOf((Class) target, n);
				o = x;
			} else
				o = null;
		} else {
			var t0 = target;
			var c0 = c.getConstructors().length != 0 ? c.getConstructors()[0] : null;
			if (c0 == null)
				c0 = c.getDeclaredConstructors().length != 0 ? c.getDeclaredConstructors()[0] : null;
			if (c0 == null)
				throw new NullPointerException(c.toString());
//			IO.println("Converter.convertMap, c0=" + c0);
			var tt = c.isRecord() ? Arrays.stream(c.getRecordComponents()).collect(Collectors.toMap(x -> x.getName(),
					x -> Reflection.actualType(x, t0), (_, x) -> x, LinkedHashMap::new)) : null;
			try {
				if (tt != null) {
					var m = map;
					var t = c;
					var oo = tt.entrySet().stream().map(x -> {
						var n2 = x.getKey();
						var t2 = x.getValue();
						if (m.containsKey(n2))
							return convert(m.get(n2), t2);
						Field f;
						try {
							f = t.getDeclaredField(n2);
						} catch (NoSuchFieldException e) {
							f = null;
						}
						return (f != null && f.isAnnotationPresent(Flat.class)) ? convertMap(m, t2, null) : null;
					}).toArray();
					try {
						o = c0.newInstance(oo);
					} catch (IllegalAccessException e) {
						c0.setAccessible(true);
						o = c0.newInstance(oo);
					}
				} else {
					o = c0.newInstance();
					for (var x : map.entrySet()) {
//						IO.println("Converter.convertMap, x=" + x);
						var n = (String) x.getKey();
						var p = !n.startsWith("$") ? Reflection.property(target, n) : null;
						if (p != null) {
							var v = convert(x.getValue(), p.genericType());
//							IO.println("Converter.convertMap, p=" + p + ", v=" + v);
							p.set(o, v);
						}
					}
				}
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
		return o;
	}
}
