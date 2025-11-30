package com.janilla.http;

public record PriorityFrame(int streamIdentifier, boolean exclusive, int streamDependency, int weight) implements Frame {
}