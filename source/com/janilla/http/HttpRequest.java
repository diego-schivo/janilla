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

import java.net.URI;

public interface HttpRequest extends HttpMessage {

	Method getMethod();

	void setMethod(Method method);

	URI getUri();

	void setUri(URI uri);

	record Method(String name) {
	}

	class Default extends HttpMessage.Default implements HttpRequest {

		protected Method method;

		protected URI uri;

		@Override
		public Method getMethod() {
			if (state == 0)
				getStartLine();
			return method;
		}

		@Override
		public void setMethod(Method method) {
			this.method = method;
			if (method != null && uri != null)
				setStartLine(method.name() + " " + uri + " HTTP/1.1");
		}

		@Override
		public URI getUri() {
			if (state == 0)
				getStartLine();
			return uri;
		}

		@Override
		public void setUri(URI uri) {
			this.uri = uri;
			if (method != null && uri != null)
				setStartLine(method.name() + " " + uri + " HTTP/1.1");
		}

		@Override
		public String getStartLine() {
			var s = state;
			var l = super.getStartLine();
			if (s == 0) {
				var ss = l != null ? l.split(" ", 3) : null;
				method = ss != null && ss.length > 0 ? new Method(ss[0].trim().toUpperCase()) : null;
				uri = ss != null && ss.length > 1 ? URI.create(ss[1].trim()) : null;
			}
			return l;
		}
	}
}
