package com.janilla.json;

public class NullTypeResolver implements TypeResolver {

	@Override
	public ObjectAndType apply(ObjectAndType t) {
		return null;
	}

	@Override
	public Class<?> parse(String string) {
		return null;
	}

	@Override
	public String format(Class<?> class1) {
		return null;
	}
}