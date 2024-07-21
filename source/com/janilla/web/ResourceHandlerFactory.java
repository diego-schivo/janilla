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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.http.HeaderField;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.io.IO;
import com.janilla.util.Lazy;

public class ResourceHandlerFactory implements WebHandlerFactory {

//	public static void main(String[] args) throws Exception {
//		var f = new ResourceHandlerFactory();
//		f.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
//				<html>
//					<head>
//						<title>My test page</title>
//					</head>
//					<body>
//						<p>My cat is very grumpy</p>
//					</body>
//				</html>""".getBytes()) : null);
//
//		var i = new ByteArrayInputStream("""
//				GET /test.html HTTP/1.1\r
//				Content-Length: 0\r
//				\r
//				""".getBytes());
//		var o = new ByteArrayOutputStream();
//		try (var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
//				var q = r.readRequest();
//				var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));
//				var s = w.writeResponse()) {
//			var c = new HttpExchange();
//			c.setRequest(q);
//			c.setResponse(s);
//			var h = f.createHandler(q, c);
//			h.handle(c);
//		}
//
//		var s = o.toString();
//		System.out.println(s);
//		assert Objects.equals(s, """
//				HTTP/1.1 200 OK\r
//				Cache-Control: max-age=3600\r
//				Content-Length: 109\r
//				\r
//				<html>
//					<head>
//						<title>My test page</title>
//					</head>
//					<body>
//						<p>My cat is very grumpy</p>
//					</body>
//				</html>""") : s;
//	}

	protected String[] packages;

	public void setPackages(String... packages) {
		this.packages = packages;
	}

	Supplier<Map<String, Resource>> resources = Lazy
			.of(() -> Arrays.stream(packages).flatMap(this::walk).collect(Collectors.toMap(r -> switch (r) {
			case FileResource x -> x.path.substring(x.package1.length() + 1);
			case ZipEntryResource x -> {
				var a = (FileResource) x.archive;
				yield a.path.substring(0, a.path.length() - 4).substring(a.package1.length() + 1) + x.path;
			}
			default -> throw new IllegalArgumentException();
			}, x -> x, (v, w) -> w, LinkedHashMap::new)));

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		var u = object instanceof HttpRequest q ? q.getUri() : null;
		var p = u != null ? u.getPath() : null;
		var r = p != null ? resources.get().get(p) : null;
		return r != null ? c -> {
			handle(r, (HttpExchange) c);
			return true;
		} : null;
	}

	static Set<String> extensions = Set.of("avif", "css", "ico", "jpg", "js", "png", "svg", "ttf", "webp", "woff",
			"woff2");

	protected Stream<Resource> walk(String package1) {
		var s = package1.replace('.', '/');
		var s1 = IntStream.iterate(s.indexOf('/'), i -> i >= 0, i -> s.indexOf('/', i + 1));
		s1 = IntStream.concat(s1, IntStream.of(s.length()));
		var s2 = s1.mapToObj(i -> s.substring(0, i));

		var l = Thread.currentThread().getContextClassLoader();
		var rr = Stream.<Resource>builder();
		try (var s3 = s2.flatMap(n -> {
//			System.out.println("n=" + n);
			return l.resources(n).map(r -> {
//				System.out.println("r=" + r);
				URI u;
				try {
					u = r.toURI();
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}

				var p = Path.of(u);
				if (n.length() < s.length())
					p = p.resolve(s.substring(n.length() + 1));

				return p;
			});
		}).distinct().filter(Files::exists)) {
			var c = IntStream.iterate(package1.indexOf('.'), i -> i >= 0, i -> package1.indexOf('.', i + 1)).count()
					+ 1;
			for (var i = s3.iterator(); i.hasNext();) {
				var p = i.next();
//				System.out.println("p=" + p);
				var q = Stream.iterate(p, Path::getParent).limit(c + 1).reduce((a, b) -> b).orElse(null);
//				System.out.println("q=" + q);
				Files.walkFileTree(p, new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						var n = file.getFileName().toString();
						var i = n.lastIndexOf('.');
						var e = i >= 0 ? n.substring(i + 1).toLowerCase() : null;
						if (e == null)
							;
						else if (extensions.contains(e)) {
							var f = q.relativize(file);
							var r = new FileResource(package1, "/" + f.toString(), Files.size(file));
//							System.out.println("r=" + r);
							rr.add(r);
						} else if (e.equals("zip")) {
							var f = q.relativize(file);
							n = f.toString();
							var a = new FileResource(package1, "/" + n, Files.size(file));
							URI u;
							try {
								u = l.getResource(n).toURI();
							} catch (URISyntaxException g) {
								throw new RuntimeException(g);
							}
							var v = u.toString();
							if (!v.startsWith("jar:"))
								u = URI.create("jar:" + v);
							var t = IO.zipFileSystem(u);
							Files.walkFileTree(t.getPath("/"), new SimpleFileVisitor<>() {

								@Override
								public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
										throws IOException {
//										System.out.println("file=" + file);
									var n = file.getFileName().toString();
									var i = n.lastIndexOf('.');
									var e = i >= 0 ? n.substring(i + 1).toLowerCase() : null;
									if (e != null && extensions.contains(e)) {
										var r = new ZipEntryResource(a, file.toString(), Files.size(file));
//										System.out.println("r=" + r);
										rr.add(r);
									}
									return FileVisitResult.CONTINUE;
								}
							});
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
//		System.out.println("rr=" + rr);
		return rr.build();
	}

	protected void handle(Resource resource, HttpExchange exchange) {
		var rs = exchange.getResponse();
//		s.setStatus(HttpResponse.Status.of(200));
		rs.setStatus(200);

		var hh = rs.getHeaders();
		if (hh == null)
			rs.setHeaders(hh = new ArrayList<>());
		hh.add(new HeaderField("cache-control", "max-age=3600"));
		switch (resource.path().substring(resource.path().lastIndexOf('.') + 1)) {
		case "ico":
			hh.add(new HeaderField("content-type", "image/x-icon"));
			break;
		case "js":
			hh.add(new HeaderField("content-type", "text/javascript"));
			break;
		case "svg":
			hh.add(new HeaderField("content-type", "image/svg+xml"));
			break;
		}
		hh.add(new HeaderField("content-length", String.valueOf(resource.size())));

		var l = Thread.currentThread().getContextClassLoader();
		try (var c = switch (resource) {
//		case FileResource x -> Channels.newChannel(l.getResourceAsStream(x.path.substring(1)));
		case FileResource x -> l.getResourceAsStream(x.path.substring(1));
		case ZipEntryResource x -> {
			URI u;
			try {
				u = l.getResource(x.archive.path().substring(1)).toURI();
			} catch (URISyntaxException g) {
				throw new RuntimeException(g);
			}
			var v = u.toString();
			if (!v.startsWith("jar:"))
				u = URI.create("jar:" + v);
//			yield Files.newByteChannel(IO.zipFileSystem(u).getPath(x.path));
			yield Files.newInputStream(IO.zipFileSystem(u).getPath(x.path));
		}
		default -> throw new IllegalArgumentException();
		}) {
//			IO.transfer(c, (WritableByteChannel) s.getBody());
//			c.transferTo((OutputStream) s.getBody());
			rs.setBody(c.readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public interface Resource {

		public String path();

		public long size();
	}

	public static record FileResource(String package1, String path, long size) implements Resource {
	}

	public static record ZipEntryResource(Resource archive, String path, long size) implements Resource {
	}
}
