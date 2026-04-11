package com.janilla.frontend;

import java.util.List;
import java.util.Map;

public record SimpleIndex(String title, Map<String, String> imports, List<Script> scripts, App app,
		List<Template> templates) implements Index {
}
