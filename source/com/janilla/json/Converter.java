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
package com.janilla.json;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
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

import com.janilla.reflect.Reflection;

public class Converter {

	protected final TypeResolver typeResolver;

	public Converter(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}

	public Object convert(Object object, Type target) {
//		System.out.println("Converter.convert, object=" + object + ", target=" + target);
		var c = getRawType(target);

		if (object == null || (object instanceof String x && x.isEmpty())) {
			if (c == Boolean.TYPE)
				return false;
			if (c == Double.TYPE)
				return 0d;
			if (c == Integer.TYPE)
				return 0;
			if (c == Long.TYPE)
				return 0l;
			if (c == String.class)
				return object;
			return null;
		}

		if (c != null && c.isAssignableFrom(object.getClass()))
			if (c != Object.class && !c.isArray() && !Collection.class.isAssignableFrom(c)
					&& !Map.class.isAssignableFrom(c))
				return object;

		if (c == BigDecimal.class)
			return switch (object) {
			case Double x -> BigDecimal.valueOf(x);
			case Integer x -> BigDecimal.valueOf(x);
			case Long x -> BigDecimal.valueOf(x);
			case String x -> new BigDecimal(x);
			default -> throw new RuntimeException();
			};

		if (c == Boolean.class || c == Boolean.TYPE)
			return switch (object) {
			case Boolean _ -> object;
			case String x -> Boolean.parseBoolean(x);
			default -> throw new RuntimeException();
			};

		if (c == Instant.class)
			return Instant.parse((String) object);

		if (c == Integer.class || c == Integer.TYPE)
			return switch (object) {
			case Integer _ -> object;
			case Long x -> x.intValue();
			case String x -> Integer.parseInt(x);
			default -> throw new RuntimeException();
			};

		if (c == LocalDate.class)
			return LocalDate.parse((String) object);

		if (c == Locale.class)
			return Locale.forLanguageTag((String) object);

		if (c == Long.class || c == Long.TYPE)
			return switch (object) {
			case Integer x -> x.longValue();
			case Long _ -> object;
			case String x -> Long.parseLong(x);
			default -> throw new RuntimeException();
			};

		if (c == OffsetDateTime.class)
			return OffsetDateTime.parse((String) object);

		if (c == Path.class)
			return Path.of((String) object);

		if (c == URI.class)
			return URI.create((String) object);

		if (c == UUID.class)
			return UUID.fromString((String) object);

		if (c == byte[].class)
			return Base64.getDecoder().decode((String) object);

		if (c == Class.class)
			return typeResolver.parse((String) object);

		if (c != null && c.isEnum()) {
			var n = switch (object) {
			case String x -> x;
			case Map<?, ?> x -> (String) x.get("name");
			default -> throw new RuntimeException();
			};
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var x = Enum.valueOf((Class) c, n);
			return x;
		}

		if (c != null && (c.isArray() || Collection.class.isAssignableFrom(c))) {
			var oo = switch (object) {
			case Object[] x -> Arrays.stream(x);
			case Collection<?> x -> x.stream();
			default -> throw new RuntimeException();
			};
			var t = c.isArray() ? c.componentType() : ((ParameterizedType) target).getActualTypeArguments()[0];
			oo = oo.map(x -> convert(x, t));

			if (c.isArray()) {
				if (c.componentType() == Double.TYPE)
					return oo.mapToDouble(x -> (double) x).toArray();
				if (c.componentType() == Integer.TYPE)
					return oo.mapToInt(x -> (int) x).toArray();
				if (c.componentType() == Long.TYPE)
					return oo.mapToLong(x -> (long) x).toArray();
				return oo.toArray(x -> (Object[]) Array.newInstance(getRawType(t), x));
			}
			if (c == List.class)
				return oo.toList();
			if (c == Set.class)
				return oo.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		if (object instanceof Map<?, ?> m) {
			if (c == Map.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				return m.entrySet().stream().map(x -> {
					var k = convert(x.getKey(), aa[0]);
					var v = convert(x.getValue(), aa[1]);
					return new AbstractMap.SimpleImmutableEntry<>(k, v);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (y, _) -> y, LinkedHashMap::new));
			}
			if (c == Map.Entry.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				var k = convert(m.get("key"), aa[0]);
				var v = convert(m.get("value"), aa[1]);
				return new AbstractMap.SimpleImmutableEntry<>(k, v);
			}
			return convertMap(m, c);
		}

		return object;
	}

	protected Object convertMap(Map<?, ?> map, Class<?> target) {
//		System.out.println("Converter.convertMap, map=" + map + ", target=" + target);
		var ot = typeResolver != null ? typeResolver.apply(new ObjectAndType(map, target)) : null;
		if (ot != null) {
			if (target != null && !target.isAssignableFrom(ot.type()))
				throw new RuntimeException();
			map = (Map<?, ?>) ot.object();
			target = ot.type();
		}

		Object o;
		if (target.isEnum()) {
			var n = (String) map.get("name");
			if (n != null && !n.isEmpty()) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var x = Enum.valueOf((Class) target, n);
				o = x;
			} else
				o = null;
		} else {
			var c0 = target.getConstructors()[0];
//			System.out.println("Converter.convertMap, c0=" + c0);
			var tt = target.isRecord() ? Arrays.stream(target.getRecordComponents()).collect(
					Collectors.toMap(x -> x.getName(), x -> x.getGenericType(), (x, _) -> x, LinkedHashMap::new))
					: null;
			try {
				if (tt != null) {
					var m = map;
					var oo = tt.entrySet().stream().map(x -> convert(m.get(x.getKey()), x.getValue())).toArray();
					o = c0.newInstance(oo);
				} else {
					o = c0.newInstance();
					for (var x : map.entrySet()) {
//						System.out.println("Converter.convertMap, x=" + x);
						var n = (String) x.getKey();
						var p = !n.startsWith("$") ? Reflection.property(target, n) : null;
						if (p != null) {
							var v = convert(x.getValue(), p.genericType());
//							System.out.println("Converter.convertMap, p=" + p + ", v=" + v);
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

	protected static Class<?> getRawType(Type type) {
//		System.out.println("Converter.getRawType, type=" + type);
		return switch (type) {
		case Class<?> x -> x;
		case ParameterizedType x -> (Class<?>) x.getRawType();
		case TypeVariable<?> _ -> null;
		default -> throw new IllegalArgumentException();
		};
	}
}
