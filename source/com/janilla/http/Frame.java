/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
import java.util.List;

public sealed interface Frame permits Frame.Data, Frame.Goaway, Frame.Headers, Frame.Ping, Frame.Priority,
		Frame.RstStream, Frame.Settings, Frame.WindowUpdate {

	int streamIdentifier();

	public enum Name {

		DATA(0x00), HEADERS(0x01), PRIORITY(0x02), RST_STREAM(0x03), SETTINGS(0x04), PUSH_PROMISE(0x05), PING(0x06),
		GOAWAY(0x07), WINDOW_UPDATE(0x08), CONTINUATION(0x09);

		private static final Name[] ALL;

		static {
			var t = Arrays.stream(values()).mapToInt(Name::type).max().getAsInt();
			ALL = new Name[t + 1];
			for (var f : values())
				ALL[f.type()] = f;
		}

		public static Name of(int type) {
			return 0 <= type && type < ALL.length ? ALL[type] : null;
		}

		private final int type;

		private Name(int type) {
			this.type = type;
		}

		public int type() {
			return type;
		}
	}

	public record Data(boolean padded, boolean endStream, int streamIdentifier, byte[] data) implements Frame {
	}

	public record Goaway(int lastStreamId, int errorCode, byte[] additionalDebugData) implements Frame {

		@Override
		public int streamIdentifier() {
			return 0;
		}
	}

	public record Headers(boolean priority, boolean endHeaders, boolean endStream, int streamIdentifier, boolean exclusive,
			int streamDependency, int weight, List<HeaderField> fields) implements Frame {
	}

	public record Ping(boolean ack, int streamIdentifier, Long opaqueData) implements Frame {
	}

	public record Priority(int streamIdentifier, boolean exclusive, int streamDependency, int weight) implements Frame {
	}

	public record RstStream(int streamIdentifier, int errorCode) implements Frame {
	}

	public record Settings(boolean ack, List<Setting.Parameter> parameters) implements Frame {

		@Override
		public int streamIdentifier() {
			return 0;
		}
	}

	public record WindowUpdate(int streamIdentifier, int windowSizeIncrement) implements Frame {
	}
}
