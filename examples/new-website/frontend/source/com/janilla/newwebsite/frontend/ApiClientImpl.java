package com.janilla.newwebsite.frontend;

import com.janilla.ioc.DiFactory;
import com.janilla.websitetemplate.frontend.WebsiteApiClient;

class ApiClientImpl extends WebsiteApiClient {

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);
	}
}
