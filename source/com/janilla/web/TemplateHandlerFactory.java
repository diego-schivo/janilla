/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.http.ExchangeContext;
import com.janilla.http.FilterHttpRequest;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.io.FilterWritableByteChannel;
import com.janilla.io.IO;
import com.janilla.util.Evaluator;
import com.janilla.util.Interpolator;

public class TemplateHandlerFactory implements HandlerFactory {

	public static void main(String[] args) throws IOException {
		var f = new TemplateHandlerFactory();
		f.setToReader(o -> {
			var r = new InputStreamReader(new ByteArrayInputStream(o.toString().getBytes()));

			var t = new Template();
			t.setReader(r);
			return t;
		});

		var o = new ByteArrayOutputStream();
		var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));

		try (var s = w.writeResponse()) {
			var h = f.createHandler("foo");
			var c = new ExchangeContext();
			c.setResponse(s);
			h.handle(c);
		}

		var s = o.toString();
		System.out.println(s);
		assert Objects.equals(s, """
				HTTP/1.1 200 OK\r
				Cache-Control: no-cache\r
				Content-Length: 4\r
				\r
				foo
				""") : s;
	}

	protected Function<Object, Template> toReader;

	protected HandlerFactory includeFactory;

	public Function<Object, Template> getToReader() {
		return toReader;
	}

	public void setToReader(Function<Object, Template> toReader) {
		this.toReader = toReader;
	}

	public HandlerFactory getIncludeFactory() {
		return includeFactory;
	}

	public void setIncludeFactory(HandlerFactory includeFactory) {
		this.includeFactory = includeFactory;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		var t = object != null ? toReader.apply(object) : null;
		return t != null ? c -> render(object, t, c) : null;
	}

	protected void render(Object object, Template template, ExchangeContext context) throws IOException {
		var h = context.getResponse().getHeaders();
		if (template.getName().endsWith(".html"))
			h.set("Content-Type", "text/html");
		else if (template.getName().endsWith(".js"))
			h.set("Content-Type", "text/javascript");

		try (var w = new PrintWriter(new BufferedWriter(Channels
				.newWriter(new FilterWritableByteChannel((WritableByteChannel) context.getResponse().getBody()) {

					@Override
					public int write(ByteBuffer src) throws IOException {

//						System.out.println(">>> " + new String(src.array(), src.position(), src.remaining()));

//						var n = 0;
//						while (src.hasRemaining())
//							n += super.write(src);
//						return n;
						return IO.repeat(x -> super.write(src), src.remaining());
					}

					@Override
					public void close() throws IOException {
					}
				}, StandardCharsets.UTF_8)))) {
			render(object, template, w, context);
		}
	}

	protected void render(Object object, Template template, PrintWriter bw, ExchangeContext context)
			throws IOException {
		var e = buildEvaluator(object, context);
		var i = buildInterpolator(e, context);
		try (var r = new BufferedReader(template.getReader())) {
			for (var j = r.lines().iterator(); j.hasNext();)
				i.println(j.next(), bw);
		}
	}

	protected Function<String, Object> buildEvaluator(Object object, ExchangeContext context) {
		var e = new Evaluator() {

			@Override
			protected Object wrap() {
				var m = new HashMap<String, Object>();
				m.put("value", context);
				return m;
			}

			@Override
			protected Object unwrap(Object wrapper) {
				return wrapper;
			}

			@Override
			protected Object value(Object wrapper) {
				@SuppressWarnings("unchecked")
				var m = (Map<String, Object>) wrapper;
				return m.get("value");
			}

			@Override
			protected void value(Object value, Object wrapper) {
				@SuppressWarnings("unchecked")
				var m = (Map<String, Object>) wrapper;
				m.put("value", value);
			}

			@Override
			protected Object invoke(Method method, Object object, Object wrapper) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) wrapper;
				m.put("method", method);
				return super.invoke(method, object, wrapper);
			}
		};
		e.setContext(object);
		return e;
	}

	protected Interpolator buildInterpolator(Function<String, Object> e, ExchangeContext context) {
		var i = new Interpolator() {

			@Override
			protected void printResult(Object object, PrintWriter writer) {
				@SuppressWarnings("unchecked")
				var m = object instanceof Map ? (Map<String, Object>) object : null;
				var n = m != null ? (Method) m.get("method") : null;
				if (n != null && n.isAnnotationPresent(Include.class)) {
					var u = (URI) m.get("value");
					if (u != null) {
						var rq = context.getRequest();
						var rq2 = new FilterHttpRequest(rq) {

							@Override
							public URI getURI() {
								return u;
							}
						};
						var h = includeFactory.createHandler(rq2);
						writer.flush();
						context.setRequest(rq2);
						try {
							h.handle(context);
						} catch (IOException x) {
							throw new UncheckedIOException(x);
						} finally {
							context.setRequest(rq);
						}
					}
				} else {
					var v = m != null ? m.get("value") : object;
					if (v instanceof Stream s)
						for (var i = s.iterator(); i.hasNext();)
							print(i.next(), writer, context);
					else
						print(v, writer, context);
				}
			}
		};
		i.setEvaluators(Map.of('$', e));
		return i;
	}

	protected void print(Object object, PrintWriter writer, ExchangeContext context) {
		var h = createHandler(object);
		if (h != null) {
			writer.flush();
			try {
				h.handle(context);
			} catch (IOException x) {
				throw new UncheckedIOException(x);
			}
		} else if (object != null) {

//			System.out.println(">>> " + object);

			writer.print(object);
		}
	}
}
