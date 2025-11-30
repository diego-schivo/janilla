package com.janilla.web;

import java.net.URI;

public record DefaultFile(Module module, URI uri, String package1, String path, long size) implements File {
}