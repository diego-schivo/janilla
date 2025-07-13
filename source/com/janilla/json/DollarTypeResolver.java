package com.janilla.json;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DollarTypeResolver implements TypeResolver {

	protected final List<Class<?>> types;

	protected final Map<String, Class<?>> parseMap = new ConcurrentHashMap<>();

	public DollarTypeResolver(List<Class<?>> types) {
		this.types = types;
	}

	@Override
	public ObjectAndType apply(ObjectAndType ot) {
		var m = ot.object() instanceof Map<?, ?> x ? x : null;
		var t = m != null ? m.get("$type") : null;
		var c = t instanceof String x ? parse(x) : null;
		return c != null ? ot.withType(c) : null;
	}

	@Override
	public Class<?> parse(String string) {
		var c = string != null && !string.isEmpty() ? parseMap.computeIfAbsent(string, k -> {
			for (var x : types)
				if (format(x).equals(k))
					return x;
			return null;
		}) : null;
//		System.out.println("DollarTypeResolver.parse, string=" + string + ", c=" + c);
		return c;
	}

	@Override
	public String format(Class<?> class1) {
		var x = class1.getName().substring(class1.getPackageName().length() + 1).replace('$', '.');
//		System.out.println("DollarTypeResolver.format, class1=" + class1 + ", x=" + x);
		return x;
	}
}