package com.janilla.addressbook.frontend;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

class ApiClientImpl extends BlankApiClient {

	protected final ContactApiClient contacts;

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);

		contacts = diFactory.newInstance(diFactory.classFor(ContactApiClient.class));
	}

	public ContactApiClient contacts() {
		return contacts;
	}
}
