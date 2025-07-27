package com.janilla.json;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class DollarTypeResolver implements TypeResolver {

	protected final Map<String, Class<?>> parseMap;

	public DollarTypeResolver(Collection<Class<?>> types) {
		parseMap = types.stream().collect(Collectors.toMap(this::format, x -> x, (x, _) -> x));
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
		var c = parseMap.get(string);
//		IO.println("DollarTypeResolver.parse, string=" + string + ", c=" + c);
		return c;
	}

	@Override
	public String format(Class<?> class1) {
		var x = class1.getName().substring(class1.getPackageName().length() + 1).replace('$', '.');
//		IO.println("DollarTypeResolver.format, class1=" + class1 + ", x=" + x);
		return x;
	}
}