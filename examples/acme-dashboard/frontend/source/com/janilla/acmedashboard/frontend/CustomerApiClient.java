package com.janilla.acmedashboard.frontend;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;

import com.janilla.acmedashboard.Customer2;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

class CustomerApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	protected final HttpExchange httpExchange;

	public CustomerApiClient(FrontendConfig config, HttpClient httpClient, Converter converter,
			HttpExchange httpExchange) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
		this.httpExchange = httpExchange;
	}

	public ListPortion<Customer2> read(String search) {
		var u = URI.create(config.api().url() + "/customers?" + new UriQueryBuilder().append("search", search));
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { Customer2.class }));
	}

	protected String cookie() {
		var r = httpExchange.request();
		var n = config.jwt().cookie();
		return r.getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; "))).filter(x -> x.startsWith(n + "="))
				.findFirst().orElse(null);
	}

}
