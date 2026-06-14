package com.janilla.acmedashboard.frontend;

import java.util.Map;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;

class ApiClientImpl extends BlankApiClient {

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);
	}

	public CustomerApiClient customers() {
		return diFactory.newInstance(CustomerApiClient.class, Map.of("httpExchange", HttpExchange.SCOPED.get()));
	}

	public DashboardApiClient dashboard() {
		return diFactory.newInstance(DashboardApiClient.class, Map.of("httpExchange", HttpExchange.SCOPED.get()));
	}

	public InvoiceApiClient invoices() {
		return diFactory.newInstance(InvoiceApiClient.class, Map.of("httpExchange", HttpExchange.SCOPED.get()));
	}
}
