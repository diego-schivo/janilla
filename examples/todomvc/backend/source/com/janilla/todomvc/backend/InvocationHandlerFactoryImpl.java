package com.janilla.todomvc.backend;

import com.janilla.blanktemplate.backend.BlankBackendInvocationHandlerFactory;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

class InvocationHandlerFactoryImpl extends BlankBackendInvocationHandlerFactory {

	public InvocationHandlerFactoryImpl(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			InvocationResolver invocationResolver, RenderableFactory renderableFactory) {
		super(config, rootFactory, diFactory, invocationResolver, renderableFactory);

		guestPost.add("/api/todo-items");
	}

	@Override
	protected boolean requireSessionEmail(HttpRequest request) {
		if (!super.requireSessionEmail(request))
			return false;

		switch (request.getHeaderValue(":method")) {
		case "DELETE", "PATCH":
			var p = webAppPath(request);
			return !(p.equals("/api/todo-items") || p.startsWith("/api/todo-items/"));
		default:
			return true;
		}
	}
}
