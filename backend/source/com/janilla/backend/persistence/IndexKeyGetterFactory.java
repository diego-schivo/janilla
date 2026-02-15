package com.janilla.backend.persistence;

public interface IndexKeyGetterFactory {

	IndexKeyGetter keyGetter(Class<?> class1, String... names);
}
