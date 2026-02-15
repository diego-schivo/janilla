package com.janilla.backend.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.java.Property;
import com.janilla.java.Reflection;

public class DefaultIndexKeyGetterFactory implements IndexKeyGetterFactory {

	@Override
	public IndexKeyGetter keyGetter(Class<?> type, String... names) {
//		IO.println("DefaultIndexKeyGetterFactory.keyGetter, type=" + type + ", names=" + Arrays.toString(names));
		class A {
			private static final Map<Class<?>, Map<List<String>, IndexKeyGetter>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(type, _ -> new ConcurrentHashMap<>()).computeIfAbsent(List.of(names),
				_ -> new DefaultIndexKeyGetter(Arrays.stream(names).map(x -> {
					var p1 = Reflection.property(type, x);
					var p2 = !p1.type().getPackageName().startsWith("java.") ? Reflection.property(p1.type(), "id")
							: null;
					return p2 != null ? Property.of(p1, p2) : p1;
				}).toArray(Property[]::new)));
	}
}
