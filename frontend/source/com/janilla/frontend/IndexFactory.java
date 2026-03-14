package com.janilla.frontend;

import com.janilla.http.HttpExchange;

public interface IndexFactory {

	Index newIndex(HttpExchange exchange);
}
