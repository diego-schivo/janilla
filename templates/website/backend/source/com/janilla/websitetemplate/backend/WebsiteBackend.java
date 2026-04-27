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
package com.janilla.websitetemplate.backend;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.smtp.SmtpClient;
import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.Handle;
import com.janilla.web.WebApp;

public class WebsiteBackend<C extends WebsiteBackendConfig> extends BlankBackend<C> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankBackend.diTypes(), Java.getPackageTypes("com.janilla.websitetemplate"),
				Java.getPackageTypes("com.janilla.websitetemplate.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { WebsiteBackend.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected final SmtpClient smtpClient;

	public WebsiteBackend(C config, DiFactory diFactory) {
		super(config, diFactory);

		var h = config.mail().host();
		smtpClient = h != null && !h.isEmpty() ? diFactory.newInstance(diFactory.classFor(SmtpClient.class),
				Map.of("host", h, "port", config.mail().port(), "username", config.mail().username(), "password",
						config.mail().password()))
				: null;
	}

	public SmtpClient smtpClient() {
		return smtpClient;
	}

	@Handle(method = "POST", path = "/api/seed")
	public void seed() throws IOException {
		((WebsitePersistence<?>) persistence).seed();
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Override
	protected boolean testDrafts(HttpExchange x) {
		var u = super.testDrafts(x) ? ((UserHttpExchange<?>) x).sessionUser() : null;
		var rr = u != null ? u.roles() : null;
		return rr != null && rr.contains(domain.userRole("ADMIN"));
	}
}
