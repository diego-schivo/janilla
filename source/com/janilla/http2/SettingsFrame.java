/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
package com.janilla.http2;

import java.util.Map;

public record SettingsFrame(boolean acknowledge, Map<SettingName, Integer> parameters) implements Frame {
}
//public class SettingsFrame {
//
//	public static void encode(SettingsFrame frame, WritableByteChannel channel) {
//		var baos = new ByteArrayOutputStream();
//		var bw = new BitsWriter(new ByteWriter(baos));
//		var fe = new FrameEncoder(FrameName.SETTINGS, bw);
//		var pp = frame.getParameters();
//		var l = (pp != null ? pp.size() : 0) * (Short.BYTES + Integer.BYTES);
//		fe.encodeLength(l);
//		fe.encodeType();
//		fe.encodeFlags(frame.isAcknowledge() ? Set.of(Flag.ACK) : null);
//		fe.encodeReserved();
//		fe.encodeStreamIdentifier(0);
//		if (l > 0)
//			for (var e : pp.entrySet())
//				fe.encodeSetting(new Setting(e.getKey(), e.getValue()));
//
//		try {
//			IO.write(baos.toByteArray(), channel);
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
//
//	private boolean acknowledge;
//
//	private Map<SettingName, Integer> parameters;
//
//	public boolean isAcknowledge() {
//		return acknowledge;
//	}
//
//	public void setAcknowledge(boolean acknowledge) {
//		this.acknowledge = acknowledge;
//	}
//
//	public Map<SettingName, Integer> getParameters() {
//		return parameters;
//	}
//
//	public void setParameters(Map<SettingName, Integer> parameters) {
//		this.parameters = parameters;
//	}
//}
