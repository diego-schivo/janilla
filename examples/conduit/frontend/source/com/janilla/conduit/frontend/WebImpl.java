package com.janilla.conduit.frontend;

import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.IndexFactory;
import com.janilla.conduit.ConduitDomain;

class WebImpl extends BlankWeb<ConduitDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, ConduitDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

}
