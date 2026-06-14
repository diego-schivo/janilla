package com.janilla.newblank.frontend;

import com.janilla.blanktemplate.frontend.BlankWeb;
import com.janilla.frontend.IndexFactory;
import com.janilla.newblank.NewBlankDomain;

class WebImpl extends BlankWeb<NewBlankDomain, ApiClientImpl> {

	WebImpl(IndexFactory indexFactory, NewBlankDomain domain, ApiClientImpl apiClient) {
		super(indexFactory, domain, apiClient);
	}

}
