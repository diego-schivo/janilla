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

import com.janilla.html.Html;
import com.janilla.http.ExchangeContext;
import com.janilla.http.FilterHttpRequest;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.FilterWritableByteChannel;
import com.janilla.io.IO;
import com.janilla.util.Evaluator;
import com.janilla.util.Interpolator;

public class TemplateHandlerFactory implements HandlerFactory {

	public static void main(String[] args) throws IOException {
		var f = new TemplateHandlerFactory();
		f.setToReader(o -> switch (o) {
		case Map<?, ?> m -> {
			var t = new Template();
			t.setReader(new InputStreamReader(new ByteArrayInputStream("foo ${x} baz".getBytes())));
			yield t;
		}
		default -> null;
		});

		var o = new ByteArrayOutputStream();
		var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));

		try (var s = w.writeResponse()) {
			var h = f.createHandler(Map.of("x", "bar"));
			var c = new ExchangeContext();
			c.setResponse(s);
			h.accept(c);
		}

		var s = o.toString();
		System.out.println(s);
		assert Objects.equals(s, """
				HTTP/1.1 200 OK\r
				Cache-Control: no-cache\r
				Content-Length: 12\r
				\r
				foo bar baz
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
	public IO.Consumer<ExchangeContext> createHandler(Object object) {
		var t = object != null ? toReader.apply(object) : null;
		return t != null ? c -> render(object, t, c) : null;
	}

	protected void render(Object object, Template template, ExchangeContext context) throws IOException {
		var s = context.getResponse();
		if (s.getStatus() == null)
			s.setStatus(new Status(200, "OK"));

		var h = s.getHeaders();
		if (h.get("Cache-Control") == null)
			s.getHeaders().set("Cache-Control", "no-cache");
		if (h.get("Content-Type") == null) {
			var n = template.getName();
			var i = n != null ? n.lastIndexOf('.') : -1;
			var e = i >= 0 ? n.substring(i + 1) : null;
			if (e != null)
				switch (e) {
				case "html":
					h.set("Content-Type", "text/html");
					break;
				case "js":
					h.set("Content-Type", "text/javascript");
					break;
				}
		}

		try (var w = new PrintWriter(
				new BufferedWriter(Channels.newWriter(new FilterWritableByteChannel((WritableByteChannel) s.getBody()) {

					@Override
					public int write(ByteBuffer src) throws IOException {

//						System.out.println(">>> " + new String(src.array(), src.position(), src.remaining()));

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
		var i = buildInterpolator(object, context);
		try (var r = new BufferedReader(template.getReader())) {
			for (var j = r.lines().iterator(); j.hasNext();)
				i.println(j.next(), bw);
		}
	}

	protected Interpolator buildInterpolator(Object object, ExchangeContext context) {
		var i = new Interpolator() {

			@Override
			protected void print(Object object, PrintWriter writer) {
				@SuppressWarnings("unchecked")
				var m = object instanceof Map ? (Map<String, Object>) object : null;
				var n = m != null ? (Method) m.get("method") : null;
				if (n != null && n.isAnnotationPresent(Include.class)) {
					var u = (URI) m.get("value");
					if (u != null) {
						var q = context.getRequest();
						var f = new FilterHttpRequest(q) {

							@Override
							public URI getURI() {
								return u;
							}
						};
						var h = includeFactory.createHandler(f);
						writer.flush();
						context.setRequest(f);
						try {
							h.accept(context);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} finally {
							context.setRequest(q);
						}
					}
				} else {
					var v = m != null ? m.get("value") : object;
					if (v instanceof Stream s)
						for (var i = s.iterator(); i.hasNext();)
							TemplateHandlerFactory.this.print(i.next(), writer, context);
					else
						TemplateHandlerFactory.this.print(v, writer, context);
				}
			}
		};
		i.setEvaluators(
				Map.of('#', buildEvaluator(object, context, false), '$', buildEvaluator(object, context, true)));
		return i;
	}

	protected Function<String, Object> buildEvaluator(Object object, ExchangeContext context, boolean escape) {
		var e = new Evaluator() {

			@Override
			public Object apply(String expression) {
				Object o = super.apply(expression);
				if (escape) {
					@SuppressWarnings("unchecked")
					var m = o instanceof Map ? (Map<String, Object>) o : null;
					var v = m != null ? m.get("value") : null;
					if (v instanceof String s)
						m.put("value", Html.escape(s));
				}
				return o;
			}

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
			protected Object getValue(Object wrapper) {
				@SuppressWarnings("unchecked")
				var m = (Map<String, Object>) wrapper;
				return m.get("value");
			}

			@Override
			protected void setValue(Object value, Object wrapper) {
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

	protected void print(Object object, PrintWriter writer, ExchangeContext context) {
		var h = createHandler(object);
		if (h != null) {
			writer.flush();
			try {
				h.accept(context);
			} catch (IOException x) {
				throw new UncheckedIOException(x);
			}
		} else if (object != null) {

//			System.out.println(">>> " + object);

			writer.print(object);
		}
	}
}
