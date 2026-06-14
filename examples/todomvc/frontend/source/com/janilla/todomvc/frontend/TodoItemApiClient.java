package com.janilla.todomvc.frontend;

import java.lang.reflect.Type;
import java.net.URI;

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.persistence.ListPortion;
import com.janilla.todomvc.TodoItem;

class TodoItemApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	public TodoItemApiClient(FrontendConfig config, HttpClient httpClient, Converter converter) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
	}

	public ListPortion<TodoItem> read() {
		var r = new HttpRequest("GET", URI.create(config.api().url() + "/todo-items"));
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { TodoItem.class }));
	}

}
