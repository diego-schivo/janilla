package com.janilla.acmedashboard.frontend;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import com.janilla.acmedashboard.Invoice;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.persistence.ListPortion;

class InvoiceApiClient {

	protected final Converter converter;

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	protected final HttpExchange httpExchange;

	public InvoiceApiClient(FrontendConfig config, HttpClient httpClient, Converter converter,
			HttpExchange httpExchange) {
		this.config = config;
		this.httpClient = httpClient;
		this.converter = converter;
		this.httpExchange = httpExchange;
	}

	public Invoice read(UUID id) {
		var u = URI.create(config.api().url() + "/invoices/" + id);
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, Invoice.class);
	}

	public ListPortion<Invoice> read(String search, Long skip, Long limit) {
		var u = URI.create(config.api().url() + "/invoices?"
				+ new UriQueryBuilder().append("search", search).append("skip", skip != null ? skip.toString() : null)
						.append("limit", limit != null ? limit.toString() : null).append("depth", String.valueOf(1)));
		var r = new HttpRequest("GET", u, cookie());
		var o = httpClient.send(r, HttpClient.JSON);
		return converter.convert(o, new SimpleParameterizedType(ListPortion.class, new Type[] { Invoice.class }));
	}

	protected String cookie() {
		var r = httpExchange.request();
		var n = config.jwt().cookie();
		return r.getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; "))).filter(x -> x.startsWith(n + "="))
				.findFirst().orElse(null);
	}

}
