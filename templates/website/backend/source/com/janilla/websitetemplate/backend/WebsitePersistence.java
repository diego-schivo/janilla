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
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.janilla.java.Configuration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.blanktemplate.backend.BlankPersistence;
import com.janilla.cms.Types;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.java.Property;
import com.janilla.json.Json;
import com.janilla.persistence.Entity;
import com.janilla.websitetemplate.SearchResult;

public class WebsitePersistence extends BlankPersistence {

	private SearchObserver<?> searchObserver;

	protected final Configuration configuration;

	protected final String configurationKey;

	public WebsitePersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
//			TypeResolver typeResolver,
			Converter converter, DiFactory diFactory, Configuration configuration, String configurationKey) {
		this.configuration = configuration;
		this.configurationKey = configurationKey;
		super(database, storables, converter, diFactory);
	}

	protected SearchObserver<?> searchObserver() {
		if (searchObserver == null)
			searchObserver = new SearchObserver<>(Arrays.stream(JavaReflect.property(SearchResult.class, "document")
					.annotatedType().getAnnotation(Types.class).value()).toList(), this);
		return searchObserver;
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			@SuppressWarnings("unchecked")
			var o = (CrudObserver<E>) searchObserver();
			c.observers().add(o);
		}
		return c;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void seed() throws IOException {
		var pp = properties();
		pp.forEach(x -> database.perform(() -> {
//			IO.println("WebsitePersistence.seed, x=" + x);
			var t = x.genericType() instanceof ParameterizedType pt ? pt.getActualTypeArguments()[0] : x.type();
			var c = crud((Class) Java.toClass(t));
			c.delete(c.list());
			return null;
		}, true));

		Object sd;
		try (var is = getClass().getResourceAsStream("seed-data.json")) {
			var s = new String(is.readAllBytes());
			sd = diFactory.newInstance(diFactory.classFor(Converter.class)).convert(Json.parse(s), seedDataClass());
		}

//		IO.println("pp=" + pp);
		pp.stream().forEach(x -> database.perform(() -> {
			var t = x.genericType() instanceof ParameterizedType pt ? pt.getActualTypeArguments()[0] : x.type();
			var c = crud((Class) Java.toClass(t));
			var o = x.get(sd);
			(o instanceof List<?> oo ? oo.stream() : Stream.of(o)).forEach(y -> c.create((Entity) y));
			return null;
		}, true));

		var r = getClass().getResource("seed-data.zip");
		URI u;
		try {
			u = r.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		if (!u.toString().startsWith("jar:"))
			u = URI.create("jar:" + u);
		var s = Java.zipFileSystem(u).getPath("/");
		var ud = configuration.getProperty(configurationKey + ".upload.directory");
		if (ud.startsWith("~"))
			ud = System.getProperty("user.home") + ud.substring(1);
		var d = Files.createDirectories(Path.of(ud));
		Files.walkFileTree(s, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var t = d.resolve(s.relativize(file).toString());
				Files.copy(file, t, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	protected Class<?> seedDataClass() {
		return SeedData.class;
	}

	protected List<Property> properties() {
		return JavaReflect.properties(seedDataClass()).collect(Collectors.toCollection(ArrayList::new));
	}
}
