package com.janilla.addressbook.frontend;

import java.lang.reflect.Type;
import java.net.URI;

import com.janilla.addressbook.Contact;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

class ContactApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	public ContactApiClient(FrontendConfig config, HttpClient httpClient, Converter converter) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
	}

	public ListPortion<Contact> read(String search) {
		var u = URI.create(config.api().url() + "/contacts?" + new UriQueryBuilder().append("search", search));
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { Contact.class }));
	}

	public Contact read1(String id) {
		var u = URI.create(config.api().url() + "/contacts/" + id);
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return converter.convert(o, Contact.class);
	}

}
