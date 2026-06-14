package com.janilla.todomvc.frontend;

import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.todomvc.TodoMvcDomain;

class WebImpl extends BlankWeb<TodoMvcDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, TodoMvcDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

	@Override
	public Index home() {
		var i = super.home();

		i.app().state().put("todoItems", apiClient.todoItems().read());

		return i;
	}

}
