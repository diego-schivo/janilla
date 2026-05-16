package com.janilla.blanktemplate.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.http.DefaultHttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Java;
import com.janilla.web.FileResource;
import com.janilla.web.Resource;
import com.janilla.web.ResourcesProvider;
import com.janilla.web.ZipEntryResource;

public class DownloadResourcesProvider implements ResourcesProvider {

	protected final String url;

	protected final BlankFrontendConfig config;

	public DownloadResourcesProvider(String url, BlankFrontendConfig config) {
		this.url = url;
		this.config = config;
	}

	@Override
	public Map<String, Resource> getResources() {
		Path d;
		{
			var s = config.download().directory();
			if (s.startsWith("~"))
				s = System.getProperty("user.home") + s.substring(1);

			var p = Path.of(s);
			if (!Files.exists(p))
				try {
					Files.createDirectories(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			d = p;
		}

		var n = url.substring(url.lastIndexOf('/') + 1);
		var f = d.resolve(n);
		if (!Files.exists(f)) {
			var l = url;
			do {
				SSLContext c;
				try {
					c = SSLContext.getDefault();
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
				l = new DefaultHttpClient(c).send(new HttpRequest("GET", URI.create(l)), rs -> {
					if (rs.getHeaderValue(":status").equals("302"))
						return rs.getHeaderValue("location");
					try {
						Files.copy(Channels.newInputStream((ReadableByteChannel) rs.getBody()), f);
						return null;
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			} while (l != null);
		}
		Resource r0;
		try {
			r0 = new FileResource(f.toString(), Files.size(f));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var fs = Java.zipFileSystem(URI.create("jar:" + r0.uri()));
		try (var pp = Files.walk(fs.getPath("/"))) {
			return pp.filter(Files::isRegularFile).collect(Collectors.toMap(x -> x.toString(), x -> {
				try {
					return new ZipEntryResource(r0, x.toString(), Files.size(x));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, (_, x) -> x, LinkedHashMap::new));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		var other = (DownloadResourcesProvider) obj;
		return Objects.equals(url, other.url);
	}
}
