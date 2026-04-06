package com.janilla.todomvc.frontend;

import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;

public class WebHandling {

	protected final IndexFactory indexFactory;

	public WebHandling(IndexFactory indexFactory) {
		this.indexFactory = indexFactory;
	}

	@Handle(method = "GET", path = "/")
	public Index home(HttpExchange exchange) {
		return indexFactory.newIndex(exchange);
	}
}
