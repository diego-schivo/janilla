package com.janilla.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import com.janilla.json.Json;
import com.janilla.reflect.Reflection;

public class EntryTree {

	Map<String, Object> tree = new LinkedHashMap<String, Object>();

	public void add(Map.Entry<String, String> t) {
		Map<String, Object> n = tree;
		var k = t.getKey().split("\\.");
		for (var i = 0; i < k.length; i++) {
			if (k[i].endsWith("]")) {
				var l = k[i].split("\\[", 2);
				var j = Integer.parseInt(l[1].substring(0, l[1].length() - 1));
				@SuppressWarnings("unchecked")
				var a = (List<Object>) n.computeIfAbsent(l[0], x -> new ArrayList<Object>());
				while (a.size() <= j)
					a.add(null);
				if (i < k.length - 1) {
					@SuppressWarnings("unchecked")
					var o = (Map<String, Object>) a.get(j);
					if (o == null) {
						o = new LinkedHashMap<String, Object>();
						a.set(j, o);
					}
					n = o;
				} else
					a.set(j, t.getValue());
			} else if (i < k.length - 1) {
				@SuppressWarnings("unchecked")
				var o = (Map<String, Object>) n.computeIfAbsent(k[i], x -> new LinkedHashMap<String, Object>());
				n = o;
			} else
				n.put(k[i], t.getValue());
		}
	}

	public <T> T convert(Class<T> target) {
		return convert(tree, target);
	}

	protected <T> T convert(Map<String, Object> tree, Class<T> target) {
		if (tree.containsKey("$type"))
			try {
				@SuppressWarnings("unchecked")
				var c = (Class<T>) Class.forName(target.getPackageName() + "." + tree.get("$type"));
				if (!target.isAssignableFrom(c))
					throw new RuntimeException();
				target = c;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

		BiFunction<String, Type, Object> c = (name, type) -> {
			var i = tree.get(name);
			var o = i != null ? switch (i) {
			case List<?> l -> {
				var u = getRawType(((ParameterizedType) type).getActualTypeArguments()[0]);
				var m = new ArrayList<>();
				for (var e : l) {
					@SuppressWarnings("unchecked")
					var f = (Map<String, Object>) e;
					m.add(convert(f, u));
				}
				yield m;
			}
			case Map<?, ?> m -> {
				@SuppressWarnings("unchecked")
				var n = (Map<String, Object>) m;
				yield convert(n, getRawType(type));
			}
			default -> Json.convert(i, getRawType(type));
			} : Json.convert(i, getRawType(type));
			return o;
		};

		try {
			if (target.isRecord()) {
				var a = new ArrayList<Object>();
				for (var d : target.getRecordComponents()) {
					var n = d.getName();
					var t = d.getGenericType();
					var b = c.apply(n, t);
					a.add(b);
				}
				@SuppressWarnings("unchecked")
				var t = (T) target.getConstructors()[0].newInstance(a.toArray());
				return t;
			}
			var z = target;
			var t = (T) z.getConstructor().newInstance();
			for (var i = Reflection.properties(z).map(n -> {
				var s = Reflection.setter(z, n);
				return s != null ? Map.entry(n, s) : null;
			}).filter(Objects::nonNull).iterator();i.hasNext();) {
				var e = i.next();
				var n = e.getKey();
				var s = e.getValue();
				var u = s.getGenericParameterTypes()[0];
				var b = c.apply(n, u);
				if (b != null)
					s.invoke(t, b);
			}
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static Class<?> getRawType(Type type) {
		return switch (type) {
		case Class<?> x -> x;
		case ParameterizedType x -> (Class<?>) x.getRawType();
		default -> throw new IllegalArgumentException();
		};
	}
}
