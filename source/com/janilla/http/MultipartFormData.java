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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.util.Util;

public record MultipartFormData(List<Part> parts, String boundary) {

	public static MultipartFormData fromByteArray(byte[] bytes) {
		var z = Util.findIndexes(bytes, "\r\n".getBytes(), 1)[0];
		var s = Arrays.copyOf(bytes, z);
		var ii = Util.findIndexes(bytes, s);
		var pp = IntStream.range(0, ii.length - 1)
				.mapToObj(x -> Arrays.copyOfRange(bytes, ii[x] + s.length + 2, ii[x + 1] - 2)).map(x -> {
					var i = Util.findIndexes(x, "\r\n\r\n".getBytes(), 1)[0];
					var hh = new String(x, 0, i).lines().map(HeaderField::fromLine).toList();
					var pb = Arrays.copyOfRange(x, i + 4, x.length);
					return new MultipartFormData.Part(hh, pb);
				}).toList();
		return new MultipartFormData(pp, new String(s, 2, s.length - 2));
	}

	public byte[] toByteArray() {
		var s1 = parts.stream().flatMap(x -> {
			var s2 = Stream.of(("--" + boundary + "\r\n").getBytes());
			var s3 = x.headers.stream().map(y -> (y.name() + ": " + y.value() + "\r\n").getBytes());
			var s4 = Stream.of("\r\n".getBytes());
			var s5 = Stream.of(x.body);
			var s6 = Stream.of("\r\n".getBytes());
			return Stream.of(s2, s3, s4, s5, s6).flatMap(y -> y);
		});
		var s7 = Stream.of(("--" + boundary + "--").getBytes());
		var bbb = Stream.concat(s1, s7).toList();
		var l = bbb.stream().mapToInt(x -> x.length).sum();
		var bb = new byte[l];
		var p = new int[] { 0 };
		bbb.stream().forEach(x -> {
			System.arraycopy(x, 0, bb, p[0], x.length);
			p[0] += x.length;
		});
		return bb;
	}

	public record Part(List<HeaderField> headers, byte[] body) {
	}
}
