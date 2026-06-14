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
package com.janilla.blanktemplate.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.janilla.backend.cms.CmsResourceHandling;
import com.janilla.backend.cms.CmsSchema;
import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.web.AbstractBackend;
import com.janilla.blanktemplate.BlankDomain;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.web.Handle;
import com.janilla.web.InvocationResolver;
import com.janilla.web.WebApp;

public class BlankBackend<C extends BlankBackendConfig, D extends BlankDomain> extends AbstractBackend<C, D> {

	private static final Logger LOGGER = System.getLogger(BlankBackend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.cms"), Java.getPackageTypes("com.janilla.http"),
				Java.getPackageTypes("com.janilla.java"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.backend", _ -> true,
						Comparator.comparingInt(x -> x.endsWith(".cms") ? 1 : 0)),
				Java.getPackageTypes("com.janilla.blanktemplate"),
				Java.getPackageTypes("com.janilla.blanktemplate.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var cfg = newConfig(new Class<?>[] { BlankBackend.class }, args.length != 0 ? args[0] : null, f);
		var ctx = (Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
		serve(a[0]);
	}

	protected CmsResourceHandling cmsResourceHandling;

	protected final Class<?> dataType;

	protected final Predicate<HttpExchange> drafts = this::testDrafts;

	protected final Class<?> seedDataClass;

	public BlankBackend(C config, DiFactory diFactory, Consumer<Object> context) {
		this(config, diFactory, context, Data.class, null);
	}

	protected BlankBackend(C config, DiFactory diFactory, Consumer<Object> context, Class<?> dataType,
			Class<?> seedDataClass) {
		this.dataType = dataType;
		this.seedDataClass = seedDataClass;
		super(config, diFactory, context);
	}

	public CmsResourceHandling cmsResourceHandling() {
		return cmsResourceHandling;
	}

	public Class<?> dataType() {
		return dataType;
	}

	public Predicate<HttpExchange> drafts() {
		return drafts;
	}

	public Class<?> seedDataClass() {
		return seedDataClass;
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Object> schema() {
		class A {
			private static final Map<Class<?>, Map<String, Object>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(dataType,
				x -> diFactory.newInstance(diFactory.classFor(CmsSchema.class), Map.of("dataType", x)));
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		if (config.upload() != null) {
			var x = config.upload().directory();
			if (x.startsWith("~"))
				x = System.getProperty("user.home") + x.substring(1);
			var d = Path.of(x);
			if (!Files.exists(d))
				try {
					Files.createDirectories(d);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			cmsResourceHandling = diFactory.newInstance(CmsResourceHandling.class, Map.of("directory", d));
		}

		return super.newInvocationResolver();
	}

	protected boolean testDrafts(HttpExchange exchange) {
		var u = exchange instanceof UserHttpExchange x ? x.sessionUser() : null;
		return u != null;
	}
}
