package com.janilla.web;

import java.lang.reflect.Method;

public record Invocation(Object object, Method method, String... regexGroups) {
}
