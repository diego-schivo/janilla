package com.janilla.petclinic.frontend;

import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.IndexFactory;
import com.janilla.petclinic.PetclinicDomain;
import com.janilla.web.Handle;
import com.janilla.web.Render;

class WebImpl extends BlankWeb<PetclinicDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, PetclinicDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

	@Handle(method = "GET", path = "/")
	public @Render(template = "welcome", resource = "/welcome.html") Object page() {
		return this;
	}

}
