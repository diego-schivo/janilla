package com.janilla.http;

import java.util.Arrays;

public enum FrameName {

	DATA(0x00), HEADERS(0x01), PRIORITY(0x02), RST_STREAM(0x03), SETTINGS(0x04), PUSH_PROMISE(0x05), PING(0x06),
	GOAWAY(0x07), WINDOW_UPDATE(0x08), CONTINUATION(0x09);

	private static final FrameName[] ALL;

	static {
		var t = Arrays.stream(values()).mapToInt(FrameName::type).max().getAsInt();
		ALL = new FrameName[t + 1];
		for (var f : values())
			ALL[f.type()] = f;
	}

	public static FrameName of(int type) {
		return 0 <= type && type < ALL.length ? ALL[type] : null;
	}

	private final int type;

	private FrameName(int type) {
		this.type = type;
	}

	public int type() {
		return type;
	}
}