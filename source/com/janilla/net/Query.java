package com.janilla.net;

import java.util.Arrays;
import java.util.stream.Stream;

public record Query(String[][] parameters) {

	public Stream<String> values(String name) {
		return Arrays.stream(parameters).filter(x -> x[0].equals(name)).map(x -> x[1]);
	}

	public static Query create(String string) {
		return new Query(Arrays.stream(string.split("&")).map(x -> x.split("=")).toArray(String[][]::new));
	}
}
