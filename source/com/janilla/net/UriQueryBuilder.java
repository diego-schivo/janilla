package com.janilla.net;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UriQueryBuilder {

	protected final StringBuilder builder = new StringBuilder();

	public UriQueryBuilder append(String name, String value) {
		if (!builder.isEmpty())
			builder.append('&');
		builder.append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=');
		if (value != null)
			builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	public static void main(String[] args) {
		var u = URI.create("/foo?" + new UriQueryBuilder().append("foo", "bar").append("baz", "quz"));
		IO.println(u.getQuery());
	}
}
