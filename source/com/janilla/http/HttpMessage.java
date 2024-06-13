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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public interface HttpMessage extends AutoCloseable {

	String getStartLine();

	void setStartLine(String startLine);

	Collection<HttpHeader> getHeaders();

	void setHeaders(Collection<HttpHeader> headers);

	Closeable getBody();

	void close();

	class Default implements HttpMessage {

		protected Raw raw;

		protected boolean useInputMode;

		protected int state;

		protected String startLine;

		protected Collection<HttpHeader> headers;

		public void setRaw(Raw raw) {
			this.raw = raw;
		}

		public void setUseInputMode(boolean useInputMode) {
			this.useInputMode = useInputMode;
		}

		@Override
		public String getStartLine() {
			if (!useInputMode)
				return startLine;
			if (state == 0) {
				startLine = raw.readStartLine();
				state = 1;
			}
			return startLine;
		}

		@Override
		public void setStartLine(String startLine) {
			if (useInputMode)
				throw new UnsupportedOperationException();
			if (state != 0)
				throw new IllegalStateException();
			this.startLine = startLine;
		}

		@Override
		public Collection<HttpHeader> getHeaders() {
			if (!useInputMode) {
				if (headers == null)
					headers = new ArrayList<>();
				return headers;
			}
			if (state < 1)
				getStartLine();
			if (state == 1) {
				headers = Stream.generate(raw::readHeader).takeWhile(x -> x != null).toList();
				state = 2;
			}
			return headers;
		}

		@Override
		public void setHeaders(Collection<HttpHeader> headers) {
			if (useInputMode)
				throw new UnsupportedOperationException();
			if (state != 0)
				throw new IllegalStateException();
			this.headers = headers;
		}

		@Override
		public Closeable getBody() {
			if (state < 2) {
				if (useInputMode)
					getHeaders();
				else {
					if (headers.stream()
							.noneMatch(x -> x.name().equals("Content-Length") || x.name().equals("Transfer-Encoding")))
						headers.add(new HttpHeader("Content-Length", "0"));
					raw.writeStartLine(startLine);
					headers.forEach(raw::writeHeader);
					state = 2;
				}
			}
			return raw.getBody();
		}

		@Override
		public void close() {
			try {
				getBody();
			} finally {
				raw.close();
			}
		}
	}

	interface Raw extends AutoCloseable {

		String readStartLine();

		void writeStartLine(String line);

		HttpHeader readHeader();

		void writeHeader(HttpHeader header);

		Closeable getBody();

		void close();
	}
}
