package com.janilla.websitetemplate.frontend;

import com.janilla.http.HttpCookie;
import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.Page;

public interface PageApiClient {

	ListPortion<Page> read(String slug, Integer depth, HttpCookie token);

}