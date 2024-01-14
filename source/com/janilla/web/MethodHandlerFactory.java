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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;
import com.janilla.util.Lazy;

public class MethodHandlerFactory implements HandlerFactory {

	public static void main(String[] args) throws IOException {
		class C {

			@SuppressWarnings("unused")
			public String foo() {
				return "bar";
			}
		}
		var c = new C();
		var f1 = new MethodHandlerFactory();
		f1.setToInvocation(r -> {
			var u = r.getURI();
			var p = u.getPath();
			var n = p.startsWith("/") ? p.substring(1) : null;
			Method m;
			try {
				m = C.class.getMethod(n);
			} catch (NoSuchMethodException e) {
				m = null;
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			}
			return m != null ? new MethodInvocation(m, c, null) : null;
		});
		var f2 = new TemplateHandlerFactory();
		f2.setToReader(o -> {
			var r = new InputStreamReader(new ByteArrayInputStream(o.toString().getBytes()));
			var t = new Template();
			t.setReader(r);
			return t;
		});
		var f = new DelegatingHandlerFactory();
		{
			var a = new HandlerFactory[] { f1, f2 };
			f.setToHandler(o -> {
				if (a != null)
					for (var g : a) {
						var h = g.createHandler(o);
						if (h != null)
							return h;
					}
				return null;
			});
		}
		f1.setRenderFactory(f);

		var is = new ByteArrayInputStream("""
				GET /foo HTTP/1.1\r
				Content-Length: 0\r
				\r
				""".getBytes());
		var rc = new HttpMessageReadableByteChannel(Channels.newChannel(is));
		var os = new ByteArrayOutputStream();
		var wc = new HttpMessageWritableByteChannel(Channels.newChannel(os));

		try (var rq = rc.readRequest(); var rs = wc.writeResponse()) {
			var h = f.createHandler(rq);
			var d = new ExchangeContext();
			d.setRequest(rq);
			d.setResponse(rs);
			h.accept(d);
		}

		var s = os.toString();
		System.out.println(s);
		assert Objects.equals(s, """
				HTTP/1.1 200 OK\r
				Cache-Control: no-cache\r
				Content-Length: 4\r
				\r
				bar
				""") : s;
	}

	protected Function<HttpRequest, MethodInvocation> toInvocation;

	protected BiFunction<MethodInvocation, ExchangeContext, Object[]> argumentsResolver;

	protected HandlerFactory renderFactory;

	public Function<HttpRequest, MethodInvocation> getToInvocation() {
		return toInvocation;
	}

	public void setToInvocation(Function<HttpRequest, MethodInvocation> toInvocation) {
		this.toInvocation = toInvocation;
	}

	public BiFunction<MethodInvocation, ExchangeContext, Object[]> getArgumentsResolver() {
		return argumentsResolver;
	}

	public void setArgumentsResolver(BiFunction<MethodInvocation, ExchangeContext, Object[]> argumentsResolver) {
		this.argumentsResolver = argumentsResolver;
	}

	public HandlerFactory getRenderFactory() {
		return renderFactory;
	}

	public void setRenderFactory(HandlerFactory renderFactory) {
		this.renderFactory = renderFactory;
	}

	@Override
	public IO.Consumer<ExchangeContext> createHandler(Object object) {
		var i = object instanceof HttpRequest q ? toInvocation.apply(q) : null;
		return i != null ? c -> handle(i, c) : null;
	}

	Supplier<BiFunction<MethodInvocation, ExchangeContext, Object[]>> argumentsResolver2 = Lazy
			.of(() -> argumentsResolver != null ? argumentsResolver : new MethodArgumentsResolver());

	protected void handle(MethodInvocation invocation, ExchangeContext context) throws IOException {
		Object[] a;
		try {
			a = argumentsResolver2.get().apply(invocation, context);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}

		Object o;
		try {
			o = a != null ? invocation.method().invoke(invocation.object(), a)
					: invocation.method().invoke(invocation.object());
		} catch (InvocationTargetException e) {
			var f = e.getTargetException();
			throw f instanceof Exception g ? new HandleException(g) : new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		var s = context.getResponse();
		if (invocation.method().getReturnType() == Void.TYPE) {
			if (s.getStatus() == null) {
				s.setStatus(new Status(204, "No Content"));
				s.getHeaders().set("Cache-Control", "no-cache");
			}
		} else if (o instanceof Path f && invocation.method().isAnnotationPresent(Attachment.class)) {
			s.setStatus(new Status(200, "OK"));
			var h = s.getHeaders();
			h.set("Cache-Control", "max-age=3600");
			h.set("Content-Disposition", "attachment; filename=\"" + f.getFileName() + "\"");
			h.set("Content-Length", String.valueOf(Files.size(f)));
			try (var fc = Files.newByteChannel(f); var bc = (WritableByteChannel) s.getBody()) {
				IO.transfer(fc, bc);
			}
		} else if (o instanceof URI v) {
			s.setStatus(new Status(302, "Found"));
			s.getHeaders().set("Cache-Control", "no-cache");
			s.getHeaders().set("Location", v.toString());
		} else {
			s.setStatus(new Status(200, "OK"));

			var h = s.getHeaders();
			if (h.get("Cache-Control") == null)
				s.getHeaders().set("Cache-Control", "no-cache");
			if (h.get("Content-Type") == null) {
				var n = context.getRequest().getURI().getPath();
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

			render(o, context);
		}
	}

	protected void render(Object object, ExchangeContext context) throws IOException {
		var h = renderFactory.createHandler(object);
		if (h != null)
			h.accept(context);
	}
}
