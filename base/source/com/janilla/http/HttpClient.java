package com.janilla.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Function;

import com.janilla.json.Json;

public interface HttpClient {

	static final Function<HttpResponse, Object> JSON = rs -> {
		try (var x = Channels.newInputStream((ReadableByteChannel) rs.getBody())) {
			var s = new String(x.readAllBytes());
//			IO.println("HttpClient.JSON, s=" + s);
			return Json.parse(s);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	};

	<R> R send(HttpRequest request, Function<HttpResponse, R> function);
}
