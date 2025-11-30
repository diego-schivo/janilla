package com.janilla.http;

public record WindowUpdateFrame(int streamIdentifier, int windowSizeIncrement) implements Frame {
}