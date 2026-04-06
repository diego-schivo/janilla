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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.backend.smtp.SmtpClient;
import com.janilla.blanktemplate.backend.BackendHttpExchange;
import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.Handle;

public class WebsiteBackend extends BlankBackend {

	public static final String[] DI_PACKAGES = Stream
			.concat(Arrays.stream(BlankBackend.DI_PACKAGES),
					Stream.of("com.janilla.websitetemplate", "com.janilla.websitetemplate.backend"))
			.toArray(String[]::new);

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageClasses(x, false).stream()).toList());
		serve(f, WebsiteBackend.class, args.length > 0 ? args[0] : null);
	}

	protected final SmtpClient smtpClient;

	public WebsiteBackend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "website-template");
	}

	public WebsiteBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
		var h = configuration.getProperty(configurationKey + ".mail.host");
		smtpClient = h != null && !h.isEmpty() ? diFactory.newInstance(diFactory.classFor(SmtpClient.class),
				Map.of("host", h, "port", Integer.parseInt(configuration.getProperty(configurationKey + ".mail.port")),
						"username", configuration.getProperty(configurationKey + ".mail.username"), "password",
						configuration.getProperty(configurationKey + ".mail.password")))
				: null;
	}

	public SmtpClient smtpClient() {
		return smtpClient;
	}

	@Handle(method = "POST", path = "/api/seed")
	public void seed() throws IOException {
		((WebsitePersistence) persistence).seed();
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Override
	protected boolean testDrafts(HttpExchange x) {
		var u = super.testDrafts(x) ? ((BackendHttpExchange) x).sessionUser() : null;
		var rr = u != null ? u.roles() : null;
		return rr != null && rr.contains(domain.userRole("ADMIN"));
	}
}
