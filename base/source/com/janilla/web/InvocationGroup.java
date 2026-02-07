package com.janilla.web;

import java.lang.reflect.Method;
import java.util.Map;

public record InvocationGroup(Object object, Map<String, Method> methods, String... regexGroups) {

	public InvocationGroup withRegexGroups(String... regexGroups) {
		return new InvocationGroup(object, methods, regexGroups);
	}
}