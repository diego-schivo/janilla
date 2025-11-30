package com.janilla.http;

public record DataFrame(boolean padded, boolean endStream, int streamIdentifier, byte[] data) implements Frame {
}