package com.janilla.todomvc.frontend;

import com.janilla.frontend.AbstractIndexFactory;
import com.janilla.frontend.Index;
import com.janilla.http.HttpExchange;
import com.janilla.web.ResourceMap;

class IndexFactoryImpl extends AbstractIndexFactory {

	public IndexFactoryImpl(ResourceMap resourceMap) {
		super(resourceMap);
	}

	@Override
	public Index newIndex(HttpExchange exchange) {
		return new IndexImpl("TodoMVC: Janilla", imports(), scripts(), new AppImpl(), templates());
	}

	@Override
	protected String baseImportKey(String name) {
		return "base/" + name;
	}
}
