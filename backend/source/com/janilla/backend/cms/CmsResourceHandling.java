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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.backend.cms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.http.HttpResponse;
import com.janilla.web.NotFoundException;

public class CmsResourceHandling {

	private static final byte[] ABSENT = {};

	private static final byte[] INVALID = {};

	protected final Path directory;

	protected final Map<String, byte[]> bodies = new ConcurrentHashMap<>();

	public CmsResourceHandling(Path directory) {
		this.directory = directory;

		try (var ee = Files.list(directory)) {
			ee.filter(Files::isRegularFile).map(x -> x.getFileName().toString()).forEach(x -> bodies.put(x, ABSENT));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Thread.startVirtualThread(this::watch);
	}

	public Path directory() {
		return directory;
	}

	public boolean canHandle(Path file) {
		return file.getNameCount() == 1 && bodies.containsKey(file.toString());
	}

	public boolean handle(Path file, HttpResponse response) {
//		IO.println("CmsResourceHandling.handle, file=" + file);

		var n = file.toString();
		var bb = bodies.get(n);

		if (bb == ABSENT)
			bb = bodies.compute(n, (_, v) -> {
				if (v == ABSENT) {
					var f = directory.resolve(file);
					if (Files.isRegularFile(f))
						try {
							v = Files.readAllBytes(f);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					else
						v = INVALID;
				}
				return v;
			});

		if (bb == INVALID)
			throw new NotFoundException();

		response.setStatus(200);
		response.setHeaderValue("cache-control", "max-age=3600");
		{
			var i = n.lastIndexOf('.');
			var e = i != -1 ? n.substring(i + 1).toLowerCase() : null;
			var t = e != null ? switch (e) {
			case "ico" -> "image/x-icon";
			case "svg" -> "image/svg+xml";
			default -> null;
			} : null;
			if (t != null)
				response.setHeaderValue("content-type", t);
		}
		response.setHeaderValue("content-length", String.valueOf(bb.length));

		try {
			((WritableByteChannel) response.getBody()).write(ByteBuffer.wrap(bb));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return true;
	}

	protected void watch() {
		try {
			var w = FileSystems.getDefault().newWatchService();
			directory.register(w, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
			for (;;)
				try {
					var k = w.take();
//					IO.println("k=" + k);
					for (var e : k.pollEvents()) {
//						IO.println(
//								"e=" + e.kind() + " " + e.count() + " " + e.context() + " " + e.context().getClass());
						var n = e.context().toString();
						if (e.kind() == StandardWatchEventKinds.ENTRY_DELETE)
							bodies.remove(n);
						else
							bodies.put(n, ABSENT);
					}
					k.reset();
//					IO.println("bodies=" + bodies);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
