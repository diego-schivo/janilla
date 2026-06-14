package com.janilla.newblank.backend;

import com.janilla.blanktemplate.backend.BlankBackendInvocationHandlerFactory;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebAppConfig;

class InvocationHandlerFactoryImpl extends BlankBackendInvocationHandlerFactory {

	public InvocationHandlerFactoryImpl(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			InvocationResolver invocationResolver, RenderableFactory renderableFactory) {
		super(config, rootFactory, diFactory, invocationResolver, renderableFactory);
	}
}
