/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.acmedashboard.frontend;

import java.nio.file.Path;
import java.util.stream.Stream;

import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.InvocationResolver;

public class AcmeDashboardFrontend extends AbstractFrontend {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.acmedashboard.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected Fetcher fetcher;

	protected HttpClient httpClient;

	public AcmeDashboardFrontend(DiFactory diFactory, Path configurationFile) {
		super(diFactory, configurationFile, "acme-dashboard");
	}

	public Fetcher fetcher() {
		return fetcher;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class));
		fetcher = diFactory.newInstance(diFactory.classFor(Fetcher.class));
		return super.newInvocationResolver();
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.acmedashboard.frontend", "");
	}
}
