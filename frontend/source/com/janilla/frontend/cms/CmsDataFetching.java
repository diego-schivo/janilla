package com.janilla.frontend.cms;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import com.janilla.cms.User;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpCookie;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

public abstract class CmsDataFetching {

	protected final String apiUrl;

	protected final HttpClient httpClient;

	protected final Converter converter;

	protected CmsDataFetching(Properties configuration, String configurationKey, HttpClient httpClient,
			DiFactory diFactory) {
		apiUrl = configuration.getProperty(configurationKey + ".api.url");
		this.httpClient = httpClient;
		converter = diFactory.newInstance(diFactory.classFor(Converter.class));
	}

	public User<?> sessionUser(HttpCookie token) {
		var o = httpClient.getJson(URI.create(apiUrl + "/users/me"), token != null ? token.format() : null);
		return converter.convert(o, User.class);
	}

	public ListPortion<User<?>> users(Long skip, Long limit) {
		var o = httpClient.getJson(URI
				.create(apiUrl + "/users?" + new UriQueryBuilder().append("skip", skip != null ? skip.toString() : null)
						.append("limit", limit != null ? limit.toString() : null)));
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, List.of(User.class)));
	}
}
