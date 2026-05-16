/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;

public class DefaultResourceHandlerFactory extends AbstractHttpHandlerFactory implements ResourceHandlerFactory {

	private static final Logger LOGGER = System.getLogger(DefaultResourceHandlerFactory.class.getName());

	protected final ResourceMap resourceMap;

	protected final Map<Resource, byte[]> bodies = new ConcurrentHashMap<>();

	public DefaultResourceHandlerFactory(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			ResourceMap resourceMap) {
		super(config, rootFactory, diFactory);
		this.resourceMap = resourceMap;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		var p = object instanceof HttpRequest r ? webAppPath(r) : null;
		var r = resourceMap != null && p != null ? resourceMap.get(p) : null;
//		IO.println("p=" + p + ", r=" + r);
		return r != null ? x -> {
			handle(r, x);
			return true;
		} : null;
	}

	protected void handle(Resource resource, HttpExchange exchange) {
		LOGGER.log(Level.DEBUG, "resource={0}", resource);

		var rs = exchange.response();
		rs.setHeaderValue(":status", "200");
		rs.setHeaderValue("cache-control", "max-age=3600");
		{
			var i = resource.path().lastIndexOf('.');
			var e = i != -1 ? resource.path().substring(i + 1).toLowerCase() : null;
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
		rs.setHeaderValue("content-length", String.valueOf(resource.size()));

		var bb = resource instanceof ZipEntryResource r && r.archive() instanceof FileResource
				? bodies.computeIfAbsent(r, _ -> {
					try {
						return r.newInputStream().readAllBytes();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				})
				: null;

		try (var in = bb != null ? new ByteArrayInputStream(bb) : resource.newInputStream();
				var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
			if (in == null)
				throw new NullPointerException(resource.toString());
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//	public static void main(String[] args) throws Exception {
//	var f = new DefaultResourceHandlerFactory();
//	f.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""".getBytes()) : null);
//
//	var i = new ByteArrayInputStream("""
//			GET /test.html HTTP/1.1\r
//			Content-Length: 0\r
//			\r
//			""".getBytes());
//	var o = new ByteArrayOutputStream();
//	try (var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
//			var q = r.readRequest();
//			var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));
//			var s = w.writeResponse()) {
//		var c = new HttpExchange();
//		c.setRequest(q);
//		c.setResponse(s);
//		var h = f.createHandler(q, c);
//		h.handle(c);
//	}
//
//	var s = o.toString();
//	IO.println(s);
//	assert Objects.equals(s, """
//			HTTP/1.1 200 OK\r
//			Cache-Control: max-age=3600\r
//			Content-Length: 109\r
//			\r
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""") : s;
//}
}
