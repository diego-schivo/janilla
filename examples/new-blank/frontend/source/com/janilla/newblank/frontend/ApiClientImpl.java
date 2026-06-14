package com.janilla.newblank.frontend;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

class ApiClientImpl extends BlankApiClient {

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);
	}
}
