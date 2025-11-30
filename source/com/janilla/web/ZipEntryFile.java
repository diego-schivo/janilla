package com.janilla.web;

public record ZipEntryFile(DefaultFile archive, String path, long size) implements File {
}