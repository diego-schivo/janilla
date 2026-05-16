package com.janilla.web;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.java.Java;

public class PackageResourcesProvider implements ResourcesProvider {

	protected static final Set<String> EXTENSIONS = Set.of("avif", "css", "html", "ico", "jpg", "js", "png", "svg",
			"ttf", "webp", "woff", "woff2");

	private static final Logger LOGGER = System.getLogger(PackageResourcesProvider.class.getName());

	protected final String package1;

	public PackageResourcesProvider(String package1) {
		this.package1 = package1;
	}

	@Override
	public Map<String, Resource> getResources() {
		var ff = Java.getPackagePaths(package1).filter(Files::isRegularFile).toList();
		Map<String, Resource> m = new LinkedHashMap<>();
		for (var f : ff) {
			Path d;
			{
				var fs = f.getFileSystem();
				d = fs == FileSystems.getDefault()
						? Stream.iterate(f, x -> x.getParent())
								.filter(x -> x.getFileName().toString().equals("classes")).findFirst().get()
						: fs.getRootDirectories().iterator().next();
			}
			f = d.relativize(f);
			LOGGER.log(Level.DEBUG, "d={0}, f={1}", d, f);

			try {
				record A(Module module, URI uri) {
					static A of(String name) {
						if (Java.class.getModule().isNamed()) {
							var l = ModuleLayer.boot();
							return l.configuration().modules().stream().flatMap(x -> {
								try (var r = x.reference().open()) {
									return r.find(name).map(y -> new A(l.findModule(x.name()).get(), y)).stream();
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							}).findFirst().orElse(null);
						} else {
							var u = Thread.currentThread().getContextClassLoader().getResource(name);
							return Optional.ofNullable(u).map(x -> {
								try {
									return new A(null, x.toURI());
								} catch (URISyntaxException e) {
									throw new RuntimeException(e);
								}
							}).orElse(null);
						}
					}
				}

				String ex;
				{
					var n = f.getFileName().toString();
					var i = n.lastIndexOf('.');
					ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
				}

				if (ex == null)
					;
				else if (EXTENSIONS.contains(ex)) {
					JavaResource r;
					{
						var n = f.toString().replace(File.separatorChar, '/');
						var mu = A.of(n);
						r = new JavaResource(mu.uri(), Files.size(d.resolve(f)), mu.module(), "/" + n);
					}

					var p = f.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
					var n = r.path().substring(p.length() + 1);
					m.put(n, r);
				} else if (ex.equals("zip")) {
					FileSystem fs;
					JavaResource r0;
					{
//						var p = f.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
						var n = f.toString().replace(File.separatorChar, '/');
						var mu = A.of(n);
						var u = mu.uri();
						if (!u.toString().startsWith("jar:"))
							u = URI.create("jar:" + u);
						fs = Java.zipFileSystem(u);
						r0 = new JavaResource(u, Files.size(d.resolve(f)), mu.module(), "/" + n);
					}

					Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							// IO.println("file=" + file);
							String ex;
							{
								var n = file.getFileName().toString();
								var i = n.lastIndexOf('.');
								ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
							}

							if (ex != null && EXTENSIONS.contains(ex)) {
								var r = new ZipEntryResource(r0, file.toString(), Files.size(file));
								// IO.println("r=" + r);
								var n = r0.path().substring(r0.path().lastIndexOf('/'), r0.path().length() - 4)
										+ r.path();
								m.put(n, r);
							}

							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		LOGGER.log(Level.DEBUG, "m={0}", m);

		return m;
	}

	@Override
	public int hashCode() {
		return Objects.hash(package1);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		var other = (PackageResourcesProvider) obj;
		return Objects.equals(package1, other.package1);
	}
}
