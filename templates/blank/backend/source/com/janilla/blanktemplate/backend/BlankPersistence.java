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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.cms.CmsPersistence;
import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.blanktemplate.Media;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.Copier;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.java.Property;
import com.janilla.json.Json;
import com.janilla.persistence.Entity;

public class BlankPersistence<C extends BlankBackendConfig> extends CmsPersistence {

	private static final Logger LOGGER = System.getLogger(BlankPersistence.class.getName());

	protected final C config;

	protected final Class<?> seedDataClass;

	protected final Copier copier;

	public BlankPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, DiFactory diFactory,
			C config, Class<?> seedDataClass, Copier copier) {
		this.config = config;
		this.seedDataClass = seedDataClass;
		this.copier = copier;
		super(database, storables, diFactory);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			Class<? extends CrudObserver<?>> t;
			if (type == Media.class)
				t = MediaCrudObserver.class;
			else
				t = null;
			if (t != null) {
				@SuppressWarnings("unchecked")
				var o = (CrudObserver<E>) diFactory.newInstance(t);
				c.observers().add(o);
			}
		}
		return c;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void seed() {
		var pp = seedProperties();

		if (pp != null && !pp.isEmpty()) {
			pp.forEach(p -> database.perform(() -> {
//			IO.println("WebsitePersistence.seed, x=" + x);
				var t = p.genericType() instanceof ParameterizedType pt ? pt.getActualTypeArguments()[0] : p.type();
				var c = crud((Class) Java.toClass(t));
				c.delete(c.list());
				return null;
			}, true));

			var d = seedData();
			d = seed(d, pp);
		}

		var r = seedDataClass.getResource("seed-data.zip");
		if (r != null) {
			URI u;
			try {
				u = r.toURI();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			if (!u.toString().startsWith("jar:"))
				u = URI.create("jar:" + u);
			var s = Java.zipFileSystem(u).getPath("/");
			var ud = config.upload().directory();
			if (ud.startsWith("~"))
				ud = System.getProperty("user.home") + ud.substring(1);
			try {
				var d = Files.createDirectories(Path.of(ud));
				Files.walkFileTree(s, new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						var t = d.resolve(s.relativize(file).toString());
						Files.copy(file, t, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	protected Object seedData() {
		Object o;
		try (var is = seedDataClass.getResourceAsStream("seed-data.json")) {
			var s = new String(is.readAllBytes());
			o = diFactory.newInstance(diFactory.classFor(Converter.class)).convert(Json.parse(s), seedDataClass);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		LOGGER.log(Level.DEBUG, "o={0}", o);
		return o;
	}

	protected List<Property> seedProperties() {
		return seedDataClass != null
				? JavaReflect.properties(seedDataClass).collect(Collectors.toCollection(ArrayList::new))
				: null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object seed(Object data, List<Property> properties) {
		return properties.stream().reduce(data, (d, p) -> database.perform(() -> {
			var t = p.genericType() instanceof ParameterizedType pt ? pt.getActualTypeArguments()[0] : p.type();
			var c = crud((Class) Java.toClass(t));
			var v = p.get(data);
			var s = (v instanceof List x ? x.stream() : Stream.of(v)).map(x -> c.create((Entity) x));
			v = v instanceof List ? s.toList() : s.findFirst().get();
			return copier.copy(Map.of(p.name(), v), d);
		}, true), (_, d) -> d);
	}
}
