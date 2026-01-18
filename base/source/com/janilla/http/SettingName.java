/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
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
