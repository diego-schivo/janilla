package com.janilla.http;

public abstract class BaseHttpExchange implements HttpExchange {

	protected final HttpRequest request;

	protected final HttpResponse response;

	protected Exception exception;

	public BaseHttpExchange(HttpRequest request, HttpResponse response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public HttpRequest request() {
		return request;
	}

	@Override
	public HttpResponse response() {
		return response;
	}

	@Override
	public Exception exception() {
		return exception;
	}
}