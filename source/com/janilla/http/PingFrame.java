package com.janilla.http;

public record PingFrame(boolean ack, int streamIdentifier, Long opaqueData) implements Frame {
}