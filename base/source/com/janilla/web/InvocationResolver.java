package com.janilla.web;

import java.util.stream.Stream;

public interface InvocationResolver {

	Stream<Invocation> lookup(String method, String path);

	Stream<InvocationGroup> groups(String path);
}
