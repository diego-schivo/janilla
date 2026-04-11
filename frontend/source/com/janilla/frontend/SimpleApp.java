package com.janilla.frontend;

import java.util.Map;

public record SimpleApp(String apiUrl, Map<String, Object> state) implements App {
}
