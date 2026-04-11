package com.janilla.java;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class AbstractConfiguration extends Properties implements Configuration {

	private static final long serialVersionUID = -4803096356071005362L;

	protected AbstractConfiguration(Class<?> class1, Path path) {
		try {
			try (var in = class1.getResourceAsStream("configuration.properties")) {
				load(in);
			}

			if (path != null)
				try (var in = Files.newInputStream(path)) {
					load(in);
				}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
