package com.janilla.http;

import java.util.Arrays;

public enum SettingName {

	HEADER_TABLE_SIZE(0x01), ENABLE_PUSH(0x02), MAX_CONCURRENT_STREAMS(0x03), INITIAL_WINDOW_SIZE(0x04),
	MAX_FRAME_SIZE(0x05), MAX_HEADER_LIST_SIZE(0x06);

	private static final SettingName[] ALL;

	static {
		var i = Arrays.stream(values()).mapToInt(SettingName::identifier).max().getAsInt();
		ALL = new SettingName[i + 1];
		for (var s : values())
			ALL[s.identifier()] = s;
	}

	public static SettingName of(int identifier) {
		return 0 <= identifier && identifier < ALL.length ? ALL[identifier] : null;
	}

	private final int identifier;

	private SettingName(int identifier) {
		this.identifier = identifier;
	}

	public int identifier() {
		return identifier;
	}
}