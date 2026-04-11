package com.janilla.http;

import com.janilla.net.Server;

public interface HttpServer extends Server {

	static final ScopedValue<HttpExchange> HTTP_EXCHANGE = ScopedValue.newInstance();

	HttpExchange createExchange(HttpRequest request, HttpResponse response);

	boolean handleExchange(HttpExchange exchange);
}
