package com.janilla.http;

import java.util.List;

public record HeadersFrame(boolean priority, boolean endHeaders, boolean endStream, int streamIdentifier,
		boolean exclusive, int streamDependency, int weight, List<HeaderField> fields) implements Frame {
}