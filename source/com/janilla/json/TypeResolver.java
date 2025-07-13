package com.janilla.json;

import java.util.function.UnaryOperator;

public interface TypeResolver extends UnaryOperator<ObjectAndType> {

	Class<?> parse(String string);

	String format(Class<?> class1);
}