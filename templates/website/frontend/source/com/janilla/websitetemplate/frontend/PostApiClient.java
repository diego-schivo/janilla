package com.janilla.websitetemplate.frontend;

import com.janilla.http.HttpCookie;
import com.janilla.persistence.ListPortion;
import com.janilla.websitetemplate.Post;

public interface PostApiClient {

	ListPortion<Post> read(String slug, Integer depth, HttpCookie token);

}