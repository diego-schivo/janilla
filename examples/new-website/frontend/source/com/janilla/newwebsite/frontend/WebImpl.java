package com.janilla.newwebsite.frontend;

import com.janilla.websitetemplate.frontend.WebsiteWeb;
import com.janilla.frontend.IndexFactory;
import com.janilla.newwebsite.NewWebsiteDomain;

class WebImpl extends WebsiteWeb<NewWebsiteDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, NewWebsiteDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

}
