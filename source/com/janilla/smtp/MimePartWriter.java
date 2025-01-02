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
package com.janilla.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public class MimePartWriter implements AutoCloseable {

	protected OutputStream output;

	protected int state;

	public MimePartWriter(OutputStream output) {
		this.output = output;
	}

	public void writeBoundary(String line) {
		if (state != 0)
			throw new IllegalStateException();
		writeLine(line);
		state = 1;
	}

	public void writeHeader(String line) {
		if (state != 1)
			throw new IllegalStateException();
		writeLine(line);
	}

	public OutputStream getBody() {
		if (state != 1)
			throw new IllegalStateException();
		writeLine("");
		state = 2;
		return output;
	}

	@Override
	public void close() {
		if (state != 2)
			throw new IllegalStateException();
		writeLine("");
	}

	protected void writeLine(String line) {
		try {
			if (!line.isEmpty())
				output.write(line.getBytes());
			output.write("\r\n".getBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
