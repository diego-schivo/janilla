package com.janilla.reflect;

import java.lang.reflect.Modifier;

public class Factory {

	protected Iterable<Class<?>> types;

	protected Object enclosing;

	public void setTypes(Iterable<Class<?>> types) {
		this.types = types;
	}

	public void setEnclosing(Object enclosing) {
		this.enclosing = enclosing;
	}

	public <T> T newInstance(Class<T> type) {
		Class<?> c = type;
		for (var t : types)
			if (!Modifier.isAbstract(t.getModifiers()) && type.isAssignableFrom(t)) {
				c = t;
				break;
			}
		try {
			@SuppressWarnings("unchecked")
			var t = (T) (c.getEnclosingClass() == enclosing.getClass() ? c.getConstructors()[0].newInstance(enclosing)
					: c.getConstructor().newInstance());
			return Reflection.copy(enclosing, t);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
