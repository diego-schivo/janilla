package com.janilla.http;

public record RstStreamFrame(int streamIdentifier, int errorCode) implements Frame {
}