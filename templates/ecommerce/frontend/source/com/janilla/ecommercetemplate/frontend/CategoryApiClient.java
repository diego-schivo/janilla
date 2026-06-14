package com.janilla.ecommercetemplate.frontend;

import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.Category;

public interface CategoryApiClient {

	ListPortion<Category> read();

}