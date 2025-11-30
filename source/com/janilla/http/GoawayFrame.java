package com.janilla.http;

public record GoawayFrame(int lastStreamId, int errorCode, byte[] additionalDebugData) implements Frame {

	@Override
	public int streamIdentifier() {
		return 0;
	}
}