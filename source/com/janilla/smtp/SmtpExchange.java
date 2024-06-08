package com.janilla.smtp;

public class SmtpExchange {

	private SmtpRequest request;

	private SmtpResponse response;

	public SmtpRequest getRequest() {
		return request;
	}

	public void setRequest(SmtpRequest value) {
		request = value;
	}

	public SmtpResponse getResponse() {
		return response;
	}

	public void setResponse(SmtpResponse value) {
		response = value;
	}
}
