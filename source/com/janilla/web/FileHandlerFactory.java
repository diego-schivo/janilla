/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.io.IO;

public class FileHandlerFactory implements HttpHandlerFactory {

	protected static final Set<String> EXTENSIONS = Set.of("avif", "css", "html", "ico", "jpg", "js", "png", "svg",
			"ttf", "webp", "woff", "woff2");

//	public static void main(String[] args) throws Exception {
//		var f = new FileHandlerFactory();
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

	protected final Map<String, File> files;

	public FileHandlerFactory(Collection<Path> files) {
//		System.out.println("FileHandlerFactory, files=" + files);
		var l = Thread.currentThread().getContextClassLoader();
		var rr = Stream.<File>builder();
		files.stream().forEach(x -> {
//			System.out.println("x.getFileSystem()=" + (x.getFileSystem() == FileSystems.getDefault()));
			var d0 = x.getFileSystem() == FileSystems.getDefault()
					? Stream.iterate(x, y -> y.getParent())
							.dropWhile(y -> !y.getFileName().toString().equals("classes")).findFirst().get()
					: x.getFileSystem().getRootDirectories().iterator().next();
			x = d0.relativize(x);
			try {
				var n = x.getFileName().toString();
				var i = n.lastIndexOf('.');
				var ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
				if (ex == null)
					;
				else if (EXTENSIONS.contains(ex)) {
//					var x = d0.relativize(file);
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '.');
					n = x.toString().replace(java.io.File.separatorChar, '/');
					var file = d0.resolve(x);
					var r = new DefaultFile(p, "/" + n, Files.size(file));
//				System.out.println("r=" + r);
					rr.add(r);
				} else if (ex.equals("zip")) {
//					var x = d0.relativize(file);
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '.');
					n = x.toString().replace(java.io.File.separatorChar, '/');
					var file = d0.resolve(x);
					var a = new DefaultFile(p, "/" + n, Files.size(file));
					URI u;
					try {
						u = l.getResource(n).toURI();
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
					var v = u.toString();
					if (!v.startsWith("jar:"))
						u = URI.create("jar:" + v);
					var fs = IO.zipFileSystem(u);
					Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//							System.out.println("file=" + file);
							var n = file.getFileName().toString();
							var i = n.lastIndexOf('.');
							var ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
							if (ex != null && EXTENSIONS.contains(ex)) {
								var r = new ZipEntryFile(a, file.toString(), Files.size(file));
//							System.out.println("r=" + r);
								rr.add(r);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		this.files = rr.build().collect(Collectors.toMap(x -> switch (x) {
		case DefaultFile y -> y.path.substring(y.package1.length() + 1);
		case ZipEntryFile y ->
			y.archive.path.substring(0, y.archive.path.length() - 4).substring(y.archive.package1.length() + 1)
					+ y.path;
		default -> throw new IllegalArgumentException();
		}, x -> x, (x, _) -> x, LinkedHashMap::new));
//		System.out.println("FileHandlerFactory, files=" + this.files);
	}

	@Override
	public HttpHandler createHandler(Object object) {
		if (object instanceof HttpRequest rq) {
			var f = files.get(rq.getPath());
			if (f != null)
				return x -> {
					handle(f, x);
					return true;
				};
		}
		return null;
	}

	protected void handle(File file, HttpExchange exchange) {
//		System.out.println("FileHandlerFactory.handle, file=" + file);
		var rs = exchange.response();
		rs.setStatus(200);
		rs.setHeaderValue("cache-control", "max-age=3600");
		switch (file.path().substring(file.path().lastIndexOf('.') + 1).toLowerCase()) {
		case "html":
			rs.setHeaderValue("content-type", "text/html");
			break;
		case "ico":
			rs.setHeaderValue("content-type", "image/x-icon");
			break;
		case "js":
			rs.setHeaderValue("content-type", "text/javascript");
			break;
		case "svg":
			rs.setHeaderValue("content-type", "image/svg+xml");
			break;
		}
		rs.setHeaderValue("content-length", String.valueOf(file.size()));

		var l = Thread.currentThread().getContextClassLoader();
		try (var in = switch (file) {
		case DefaultFile x -> l.getResourceAsStream(x.path.substring(1));
		case ZipEntryFile x -> {
			URI u;
			try {
				u = l.getResource(x.archive.path.substring(1)).toURI();
			} catch (URISyntaxException g) {
				throw new RuntimeException(g);
			}
//			System.out.println("FileHandlerFactory.handle, u=" + u);
			var s = u.toString();
			if (!s.startsWith("jar:"))
				u = URI.create("jar:" + s);
			var p = IO.zipFileSystem(u).getPath(x.path);
//			System.out.println("FileHandlerFactory.handle, p=" + p);
			yield Files.newInputStream(p);
		}
		default -> throw new IllegalArgumentException();
		}; var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public interface File {

		String path();

		long size();
	}

	public record DefaultFile(String package1, String path, long size) implements File {
	}

	public record ZipEntryFile(DefaultFile archive, String path, long size) implements File {
	}
}
