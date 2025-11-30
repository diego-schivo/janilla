package com.janilla.http;

import java.util.List;

public record SettingsFrame(boolean ack, List<SettingParameter> parameters) implements Frame {

	@Override
	public int streamIdentifier() {
		return 0;
	}
}