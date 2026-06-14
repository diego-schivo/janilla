package com.janilla.todomvc.frontend;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

class ApiClientImpl extends BlankApiClient {

	protected final TodoItemApiClient todoItems;

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);

		todoItems = diFactory.newInstance(diFactory.classFor(TodoItemApiClient.class));
	}

	public TodoItemApiClient todoItems() {
		return todoItems;
	}

}
