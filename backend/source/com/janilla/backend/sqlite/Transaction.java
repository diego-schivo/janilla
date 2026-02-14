package com.janilla.backend.sqlite;

import java.util.List;

public record Transaction(List<Range> writeRanges, long startSize) {
}