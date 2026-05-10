/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.blanktemplate.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.DefaultHttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.java.Java;

public class DownloadHandlerFactory implements HttpHandlerFactory {

	private static final Logger LOGGER = System.getLogger(DownloadHandlerFactory.class.getName());

	protected final Map<String, Path> files;

	protected final Map<String, byte[]> bodies = new ConcurrentHashMap<>();

	public DownloadHandlerFactory(BlankFrontendConfig config, String configurationKey) {
		var d = config.download().directory();
		if (d.startsWith("~"))
			d = System.getProperty("user.home") + d.substring(1);
		Path d2;
		{
			var x = Path.of(d);
			if (!Files.exists(x))
				try {
					Files.createDirectories(x);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			d2 = x;
		}
		files = Stream
				.of("https://github.com/vercel/geist-font/releases/download/1.8.0/geist-font-1.8.0.zip",
						"https://github.com/lucide-icons/lucide/releases/download/1.14.0/lucide-icons-1.14.0.zip")
				.map(x -> {
					var n = x.substring(x.lastIndexOf('/') + 1);
					var f = d2.resolve(n);
					if (!Files.exists(f)) {
						var l = x;
						do {
							SSLContext c;
							try {
								c = SSLContext.getDefault();
							} catch (NoSuchAlgorithmException e) {
								throw new RuntimeException(e);
							}
							l = new DefaultHttpClient(c).send(new HttpRequest("GET", URI.create(l)), rs -> {
								if (rs.getHeaderValue(":status").equals("302"))
									return rs.getHeaderValue("location");
								try {
									Files.copy(Channels.newInputStream((ReadableByteChannel) rs.getBody()), f);
									return null;
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							});
						} while (l != null);
					}
					return f;
				}).flatMap(x -> {
					var fs = Java.zipFileSystem(URI.create("jar:file://" + x));
					try (var pp = Files.walk(fs.getPath("/"))) {
						return pp.filter(Files::isRegularFile).toList().stream();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).collect(Collectors.toMap(x -> x.toString(), x -> x));
		LOGGER.log(Level.DEBUG, "files={0}", files);
	}

	@Override
	public HttpHandler createHandler(Object object) {
		var f = files != null && object instanceof HttpRequest r ? files.get(r.getPath()) : null;
		return f != null ? x -> {
			handle(f, x);
			return true;
		} : null;
	}

	protected void handle(Path file, HttpExchange exchange) {
//		IO.println("FileHandlerFactory.handle, file=" + file);

		var bb = bodies.computeIfAbsent(file.toString(), _ -> {
			try {
				return Files.readAllBytes(file);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		var rs = exchange.response();
		rs.setHeaderValue(":status", "200");
		rs.setHeaderValue("cache-control", "max-age=3600");
		{
			var n = file.getFileName().toString();
			var i = n.lastIndexOf('.');
			var e = i != -1 ? n.substring(i + 1).toLowerCase() : null;
			var t = e != null ? switch (e) {
			case "html" -> "text/html";
			case "ico" -> "image/x-icon";
			case "js" -> "text/javascript";
			case "svg" -> "image/svg+xml";
			default -> null;
			} : null;
			if (t != null)
				rs.setHeaderValue("content-type", t);
		}
		rs.setHeaderValue("content-length", String.valueOf(bb.length));

		try {
			((WritableByteChannel) rs.getBody()).write(ByteBuffer.wrap(bb));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
