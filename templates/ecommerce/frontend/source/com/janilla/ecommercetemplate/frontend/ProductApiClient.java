package com.janilla.ecommercetemplate.frontend;

import com.janilla.ecommercetemplate.Product;
import com.janilla.http.HttpCookie;
import com.janilla.persistence.ListPortion;

public interface ProductApiClient {

	ListPortion<Product> read(String slug, String query, Long category, String sort, Integer depth, HttpCookie token);

}