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
package com.janilla.net;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UriQueryBuilder {

	protected final StringBuilder builder = new StringBuilder();

	public UriQueryBuilder append(String name, String value) {
		if (value != null) {
			if (!builder.isEmpty())
				builder.append('&');
			builder.append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=')
					.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		}
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	public static void main(String[] args) {
		var u = URI.create("/foo?" + new UriQueryBuilder().append("foo", "bar").append("baz", "quz"));
		IO.println(u.getQuery());
	}
}
