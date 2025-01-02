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
import java.math.BigDecimal;
import java.net.URI;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.reflect.Reflection;

public class Converter {

	protected UnaryOperator<MapType> resolver;

	public void setResolver(UnaryOperator<MapType> resolver) {
		this.resolver = resolver;
	}

	public Object convert(Object input, Type target) {
		var r = getRawType(target);

		if (input == null || (input instanceof String s && s.isEmpty())) {
			if (r == Boolean.TYPE)
				return false;
			if (r == Integer.TYPE)
				return 0;
			if (r == Long.TYPE)
				return 0l;
			if (r == String.class)
				return input;
			return null;
		}

		if (r != Object.class && r.isAssignableFrom(input.getClass()) && !r.isArray()
				&& !Collection.class.isAssignableFrom(r) && !Map.class.isAssignableFrom(r))
			return input;

		if (r == BigDecimal.class)
			return switch (input) {
			case Double x -> BigDecimal.valueOf(x);
			case Long x -> BigDecimal.valueOf(x);
			case String x -> new BigDecimal(x);
			default -> throw new RuntimeException();
			};
		if (r == Boolean.class || r == Boolean.TYPE)
			return switch (input) {
			case Boolean _ -> input;
			case String x -> Boolean.parseBoolean(x);
			default -> throw new RuntimeException();
			};
		if (r == Instant.class)
			return Instant.parse((String) input);
		if (r == Integer.class || r == Integer.TYPE)
			return switch (input) {
			case Integer _ -> input;
			case Long x -> x.intValue();
			case String x -> Integer.parseInt(x);
			default -> throw new RuntimeException();
			};
		if (r == LocalDate.class)
			return LocalDate.parse((String) input);
		if (r == Locale.class)
			return Locale.forLanguageTag((String) input);
		if (r == Long.class || r == Long.TYPE)
			return switch (input) {
			case Integer x -> x.longValue();
			case Long _ -> input;
			case String x -> Long.parseLong(x);
			default -> throw new RuntimeException();
			};
		if (r == OffsetDateTime.class)
			return OffsetDateTime.parse((String) input);
		if (r == URI.class)
			return URI.create((String) input);
		if (r == UUID.class)
			return UUID.fromString((String) input);
		if (r == byte[].class)
			return Base64.getDecoder().decode((String) input);
		if (r == long[].class)
			return ((Collection<?>) input).stream().mapToLong(x -> (long) x).toArray();

		if (r.isEnum())
			return Stream.of(r.getEnumConstants()).filter(x -> x.toString().equals(input)).findFirst().orElseThrow();

		if (r.isArray() || Collection.class.isAssignableFrom(r)) {
			var s = switch (input) {
			case Object[] x -> Arrays.stream(x);
			case Collection<?> x -> x.stream();
			default -> throw new RuntimeException();
			};
			var t = r.isArray() ? r.componentType() : ((ParameterizedType) target).getActualTypeArguments()[0];
			s = s.map(y -> convert(y, t));

			if (r.isArray()) {
				if (r.componentType() == Integer.TYPE)
					return s.mapToInt(x -> (Integer) x).toArray();
				else
					return s.toArray(l -> (Object[]) Array.newInstance(getRawType(t), l));
			}
			if (r == List.class)
				return s.toList();
			if (r == Set.class)
				return s.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		if (input instanceof Map<?, ?> n) {
			if (r == Map.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				return n.entrySet().stream().map(e -> {
					var k = convert(e.getKey(), aa[0]);
					var v = convert(e.getValue(), aa[1]);
					return new AbstractMap.SimpleEntry<>(k, v);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}
			if (r == Map.Entry.class) {
				var aa = ((ParameterizedType) target).getActualTypeArguments();
				var k = convert(n.get("key"), aa[0]);
				var v = convert(n.get("value"), aa[1]);
				return new AbstractMap.SimpleEntry<>(k, v);
			}
			return convert(n, r);
		}

		return input;
	}

	protected Object convert(Map<?, ?> input, Class<?> target) {
		var r = resolver != null ? resolver.apply(new MapType(input, target)) : null;
		if (r != null) {
			if (!target.isAssignableFrom(r.type))
				throw new RuntimeException();
			target = r.type;
			input = r.map;
		}

		// System.out.println("c=" + c);
		var d = target.getConstructors()[0];
		// System.out.println("d=" + d);
		var tt = target.isRecord()
				? Arrays.stream(target.getRecordComponents()).collect(
						Collectors.toMap(x -> x.getName(), x -> x.getGenericType(), (x, _) -> x, LinkedHashMap::new))
				: null;
		Object o;
		try {
			if (tt != null) {
				var m = input;
				var z = tt.entrySet().stream().map(x -> convert(m.get(x.getKey()), x.getValue())).toArray();
				o = d.newInstance(z);
			} else {
				o = d.newInstance();
				for (var e : input.entrySet()) {
					var n = (String) e.getKey();
					if (n.startsWith("$"))
						continue;
					// System.out.println("e=" + e);
					var s = Reflection.property(target, n);
					var v = convert(e.getValue(), s.genericType());
					// System.out.println("s=" + s + ", i=" + i + ", v=" + v);
					s.set(o, v);
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		return o;
	}

	static Class<?> getRawType(Type type) {
		return switch (type) {
		case Class<?> x -> x;
		case ParameterizedType x -> (Class<?>) x.getRawType();
		default -> throw new IllegalArgumentException();
		};
	}

	public record MapType(Map<?, ?> map, Class<?> type) {
	}
}
