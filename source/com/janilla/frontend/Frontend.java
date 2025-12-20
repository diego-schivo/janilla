package com.janilla.frontend;

import java.util.Map;
import java.util.stream.Stream;

public class Frontend {

	public static void putImports(Map<String, String> map) {
		Stream.of("web-component").forEach(x -> map.put(x, "/" + x + ".js"));
	}
}
