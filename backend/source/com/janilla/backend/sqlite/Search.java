package com.janilla.backend.sqlite;

import java.util.stream.Stream;

public record Search(BTreePath path, int found, Stream<Stream<Object>> rows) {

	public Search withPath(BTreePath path) {
		return new Search(path, found, rows);
	}

	public Search withFound(int found) {
		return new Search(path, found, rows);
	}

	public Search withRows(Stream<Stream<Object>> rows) {
		return new Search(path, found, rows);
	}
}