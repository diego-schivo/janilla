package com.janilla.web;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Map;

public record InvocationGroup(Object object, Map<Method, MethodHandle> methodHandles, String... regexGroups) {

	public InvocationGroup withRegexGroups(String... regexGroups) {
		return new InvocationGroup(object, methodHandles, regexGroups);
	}
}
