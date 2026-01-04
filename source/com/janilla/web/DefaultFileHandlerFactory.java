/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.java.Java;

public class DefaultFileHandlerFactory implements FileHandlerFactory {

	protected static final Set<String> EXTENSIONS = Set.of("avif", "css", "html", "ico", "jpg", "js", "png", "svg",
			"ttf", "webp", "woff", "woff2");

	protected final Map<String, File> files;

	public DefaultFileHandlerFactory(List<Path> files) {
//		IO.println("FileHandlerFactory, files=" + files);
		var rr = Stream.<File>builder();
		files.stream().forEach(x -> {
//			IO.println("x.getFileSystem()=" + (x.getFileSystem() == FileSystems.getDefault()));
			var d = x.getFileSystem() == FileSystems.getDefault()
					? Stream.iterate(x, y -> y.getParent())
							.dropWhile(y -> !y.getFileName().toString().equals("classes")).findFirst().get()
					: x.getFileSystem().getRootDirectories().iterator().next();
			x = d.relativize(x);
			try {
				var n0 = x.getFileName().toString();
				var i = n0.lastIndexOf('.');
				var ex = i != -1 ? n0.substring(i + 1).toLowerCase() : null;
				if (ex == null)
					;
				else if (EXTENSIONS.contains(ex)) {
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '.');
					var n = x.toString().replace(java.io.File.separatorChar, '/');
					class B {

						private Module m;

						private URI u;
					}
					var b = ModuleLayer.boot().configuration().modules().stream().map(ResolvedModule::reference)
							.map(y -> {
								try (var r = y.open()) {
									var o = r.find(n);
									if (o.isEmpty())
										return null;
									var z = new B();
									z.u = o.get();
									z.m = ModuleLayer.boot().findModule(y.descriptor().name()).get();
									return z;
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							}).filter(Objects::nonNull).findFirst().get();
					var f = d.resolve(x);
					var r = new DefaultFile(b.m, b.u, p, "/" + n, Files.size(f));
//					IO.println("r=" + r);
					rr.add(r);
				} else if (ex.equals("zip")) {
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '.');
					var n = x.toString().replace(java.io.File.separatorChar, '/');
					class B {

						private Module m;

						private URI u;
					}
					var b = ModuleLayer.boot().configuration().modules().stream().map(ResolvedModule::reference)
							.map(y -> {
								try (var r = y.open()) {
									var o = r.find(n);
									if (o.isEmpty())
										return null;
									var z = new B();
									z.u = o.get();
									z.m = ModuleLayer.boot().findModule(y.descriptor().name()).get();
									return z;
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							}).filter(Objects::nonNull).findFirst().get();
					var v = b.u.toString();
					if (!v.startsWith("jar:"))
						b.u = URI.create("jar:" + v);
					var fs = Java.zipFileSystem(b.u);
					var f = d.resolve(x);
					var a = new DefaultFile(b.m, b.u, p, "/" + n, Files.size(f));
					Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//							IO.println("file=" + file);
							var n = file.getFileName().toString();
							var i = n.lastIndexOf('.');
							var ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
							if (ex != null && EXTENSIONS.contains(ex)) {
								var r = new ZipEntryFile(a, file.toString(), Files.size(file));
//								IO.println("r=" + r);
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
		case DefaultFile y -> y.path().substring(y.package1().length() + 1);
		case ZipEntryFile y -> y.archive().path().substring(0, y.archive().path().length() - 4)
				.substring(y.archive().package1().length() + 1) + y.path();
		default -> throw new IllegalArgumentException();
		}, x -> x, (_, x) -> x, LinkedHashMap::new));
//		IO.println("FileHandlerFactory, files=" + this.files);
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
//		IO.println("FileHandlerFactory.handle, file=" + file);
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

		try (var in = switch (file) {
		case DefaultFile x -> x.module().getResourceAsStream(x.path().substring(1));
		case ZipEntryFile x -> {
			var u = x.archive().uri();
//			IO.println("FileHandlerFactory.handle, u=" + u);
			var s = u.toString();
			if (!s.startsWith("jar:"))
				u = URI.create("jar:" + s);
			var p = Java.zipFileSystem(u).getPath(x.path());
//			IO.println("FileHandlerFactory.handle, p=" + p);
			yield Files.newInputStream(p);
		}
		default -> throw new IllegalArgumentException();
		}; var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//	public static void main(String[] args) throws Exception {
//	var f = new FileHandlerFactory();
//	f.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""".getBytes()) : null);
//
//	var i = new ByteArrayInputStream("""
//			GET /test.html HTTP/1.1\r
//			Content-Length: 0\r
//			\r
//			""".getBytes());
//	var o = new ByteArrayOutputStream();
//	try (var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
//			var q = r.readRequest();
//			var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));
//			var s = w.writeResponse()) {
//		var c = new HttpExchange();
//		c.setRequest(q);
//		c.setResponse(s);
//		var h = f.createHandler(q, c);
//		h.handle(c);
//	}
//
//	var s = o.toString();
//	IO.println(s);
//	assert Objects.equals(s, """
//			HTTP/1.1 200 OK\r
//			Cache-Control: max-age=3600\r
//			Content-Length: 109\r
//			\r
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""") : s;
//}
}
