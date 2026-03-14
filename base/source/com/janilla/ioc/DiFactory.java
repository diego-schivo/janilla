package com.janilla.ioc;

import java.util.List;
import java.util.Map;

public interface DiFactory {

	Object context();

	DiFactory context(Object context);

	List<Class<?>> types();

	<T, U extends T> Class<U> classFor(Class<T> type);

	default <T> T newInstance(Class<T> class1) {
		return newInstance(class1, null);
	}

	<T> T newInstance(Class<T> class1, Map<String, Object> arguments);
}
