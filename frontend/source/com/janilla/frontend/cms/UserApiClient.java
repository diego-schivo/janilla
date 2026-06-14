package com.janilla.frontend.cms;

import com.janilla.cms.User;
import com.janilla.http.HttpCookie;
import com.janilla.persistence.ListPortion;

public interface UserApiClient {

	User<?> sessionUser(HttpCookie token);

	ListPortion<User<?>> users(Long skip, Long limit);

}