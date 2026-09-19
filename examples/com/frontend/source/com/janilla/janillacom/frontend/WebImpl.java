package com.janilla.janillacom.frontend;

import com.janilla.websitetemplate.frontend.WebsiteWeb;
import com.janilla.frontend.IndexFactory;
import com.janilla.janillacom.JanillaDomain;

class WebImpl extends WebsiteWeb<JanillaDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, JanillaDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

}
