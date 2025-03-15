package com.janilla.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public record MapAndType(Map<?, ?> map, Class<?> type) {

	public interface TypeResolver extends UnaryOperator<MapAndType> {
	}

	public static class NullTypeResolver implements TypeResolver {

		@Override
		public MapAndType apply(MapAndType mt) {
			return null;
		}
	}

	public static class DollarTypeResolver implements TypeResolver {

		protected final Iterable<Class<?>> types;

		protected final Map<String, Class<?>> resolveMap = new ConcurrentHashMap<>();

		public DollarTypeResolver(Iterable<Class<?>> types) {
			this.types = types;
		}

		@Override
		public MapAndType apply(MapAndType mt) {
			var t = (String) mt.map().get("$type");
//			System.out.println("TypeResolver.apply, t = " + t);
			if (t == null)
				return null;
			var c = resolveMap.computeIfAbsent(t, k -> {
				for (var x : types) {
					var n = x.getName().substring(x.getPackageName().length() + 1).replace('$', '.');
//					System.out.println("TypeResolver.apply, n = " + n);
					if (n.equals(k))
						return x;
				}
				return null;
			});
//			System.out.println("TypeResolver.apply, c = " + c);
			return c != null ? new MapAndType(mt.map(), c) : null;
		}
	}
}
