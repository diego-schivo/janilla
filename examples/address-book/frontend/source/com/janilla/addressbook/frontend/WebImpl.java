package com.janilla.addressbook.frontend;

import com.janilla.addressbook.AddressBookDomain;
import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.web.Handle;

class WebImpl extends BlankWeb<AddressBookDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, AddressBookDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

	@Handle(method = "GET", path = "/")
	public Index home(String q) {
		var i = indexFactory.newIndex();
		i.app().state().put("contacts", apiClient.contacts().read(q).elements());

		return i;
	}

	@Handle(method = "GET", path = "/contacts/([^/]+)(/edit)?")
	public Index contact(String id, String edit, String q) {
		var i = indexFactory.newIndex();
		i.app().state().put("contacts", apiClient.contacts().read(q).elements());
		i.app().state().put("contact", apiClient.contacts().read1(id));

		return i;
	}

	@Handle(method = "GET", path = "/about")
	public Index about() {
		return indexFactory.newIndex();
	}

}
