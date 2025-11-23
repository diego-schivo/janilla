package com.janilla.web;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public record Invocation(Object object, Method method, MethodHandle methodHandle, Type[] parameterTypes,
		String... regexGroups) {
}
