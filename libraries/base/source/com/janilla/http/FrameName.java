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

public enum FrameName {

	DATA(0), HEADERS(1), PRIORITY(2), RST_STREAM(3), SETTINGS(4), PUSH_PROMISE(5), PING(6), GOAWAY(7), WINDOW_UPDATE(8),
	CONTINUATION(9);

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
