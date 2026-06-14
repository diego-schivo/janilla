package com.janilla.acmedashboard.frontend;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.janilla.acmedashboard.Cards;
import com.janilla.acmedashboard.Invoice;
import com.janilla.acmedashboard.Revenue;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;

class DashboardApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	protected final HttpExchange httpExchange;

	public DashboardApiClient(FrontendConfig config, HttpClient httpClient, Converter converter,
			HttpExchange httpExchange) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
		this.httpExchange = httpExchange;
	}

	public Cards cards() {
		var u = URI.create(config.api().url() + "/dashboard/cards");
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, Cards.class);
	}

	public List<Invoice> invoices() {
		var u = URI.create(config.api().url() + "/dashboard/invoices");
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(List.class, new Type[] { Invoice.class }));
	}

	public List<Revenue> revenue() {
		var u = URI.create(config.api().url() + "/dashboard/revenue");
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(List.class, new Type[] { Revenue.class }));
	}

	protected String cookie() {
		var r = httpExchange.request();
		var n = config.jwt().cookie();
		return r.getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; "))).filter(x -> x.startsWith(n + "="))
				.findFirst().orElse(null);
	}

}
