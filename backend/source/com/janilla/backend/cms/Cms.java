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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import com.janilla.http.HttpRequest;
import com.janilla.http.MultipartFormData;

public class Cms {

	public static Map<String, byte[]> files(HttpRequest request) throws IOException {
		byte[][] bbb;
		{
			var b = request.getHeaderValue("content-type").split(";")[1].trim().substring("boundary=".length());
			var ch = (ReadableByteChannel) request.getBody();
			var bb = Channels.newInputStream(ch).readAllBytes();
			var s = ("--" + b).getBytes();
			var ii = MultipartFormData.findIndexes(bb, s);
			bbb = IntStream.range(0, ii.length - 1)
					.mapToObj(i -> Arrays.copyOfRange(bb, ii[i] + s.length + 2, ii[i + 1] - 2)).toArray(byte[][]::new);
		}
		Map<String, byte[]> m = new LinkedHashMap<>();
		for (var bb : bbb) {
			var i = MultipartFormData.findIndexes(bb, "\r\n\r\n".getBytes(), 1)[0];
			var cd = Arrays.stream(new String(bb, 0, i).split("\r\n"))
					.map(s -> Arrays.stream(s.split(":", 2)).map(String::trim).toArray(String[]::new))
					.filter(x -> x[0].equalsIgnoreCase("Content-Disposition")).findFirst().get()[1];
			var n = Arrays.stream(cd.split(";")).map(String::trim).filter(x -> x.startsWith("filename="))
					.map(x -> x.substring(x.indexOf('=') + 1))
					.map(x -> x.startsWith("\"") && x.endsWith("\"") ? x.substring(1, x.length() - 1) : x).findFirst()
					.orElseThrow();
			bb = Arrays.copyOfRange(bb, i + 4, bb.length);
			m.put(n, bb);
		}
		return m;
	}
}
