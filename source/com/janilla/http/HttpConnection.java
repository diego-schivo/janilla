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
package com.janilla.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janilla.net.Connection;

public class HttpConnection extends Connection {

	private HeaderDecoder headerDecoder = new HeaderDecoder();

	private boolean prefaceReceived;

	private boolean prefaceSent;

	private Map<Integer, List<Frame>> streams = new HashMap<>();

	// *******************
	// Getters and Setters

	public HeaderDecoder getHeaderDecoder() {
		return headerDecoder;
	}

	public void setHeaderDecoder(HeaderDecoder headerDecoder) {
		this.headerDecoder = headerDecoder;
	}

	public boolean isPrefaceReceived() {
		return prefaceReceived;
	}

	public void setPrefaceReceived(boolean prefaceReceived) {
		this.prefaceReceived = prefaceReceived;
	}

	public boolean isPrefaceSent() {
		return prefaceSent;
	}

	public void setPrefaceSent(boolean prefaceSent) {
		this.prefaceSent = prefaceSent;
	}

	public Map<Integer, List<Frame>> getStreams() {
		return streams;
	}

	public void setStreams(Map<Integer, List<Frame>> streams) {
		this.streams = streams;
	}

	// Getters and Setters
	// *******************
}
