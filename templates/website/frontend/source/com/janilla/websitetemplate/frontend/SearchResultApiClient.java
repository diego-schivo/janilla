package com.janilla.websitetemplate.frontend;

import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.SearchResult;

public interface SearchResultApiClient {

	ListPortion<SearchResult> read(String query);

}