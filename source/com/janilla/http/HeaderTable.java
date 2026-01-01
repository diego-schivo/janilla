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
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderTable {

	protected static final HeaderTable STATIC;

	static {
		STATIC = new HeaderTable(false, 1);
		"""
				:authority
				:method: GET
				:method: POST
				:path: /
				:path: /index.html
				:scheme: http
				:scheme: https
				:status: 200
				:status: 204
				:status: 206
				:status: 304
				:status: 400
				:status: 404
				:status: 500
				accept-charset
				accept-encoding: gzip
				accept-language
				accept-ranges
				accept
				access-control-allow-origin
				age
				allow
				authorization
				cache-control
				content-disposition
				content-encoding
				content-language
				content-length
				content-location
				content-range
				content-type
				cookie
				date
				etag
				expect
				expires
				from
				host
				if-match
				if-modified-since
				if-none-match
				if-range
				if-unmodified-since
				last-modified
				link
				location
				max-forwards
				proxy-authenticate
				proxy-authorization
				range
				referer
				refresh
				retry-after
				server
				set-cookie
				strict-transport-security
				transfer-encoding
				user-agent
				vary
				via
				www-authenticate: """.lines().map(x -> {
			var i = x.indexOf(':', 1);
			return new HeaderField(i != -1 ? x.substring(0, i).trim() : x, i != -1 ? x.substring(i + 1).trim() : null);
		}).forEach(STATIC::add);
	}

	protected static int size(HeaderField header) {
		return header.name().length() + (header.value() != null ? header.value().length() : 0) + 32;
	}

	protected final boolean dynamic;

	protected final int startIndex;

	protected int maxSize = -1;

	protected int size;

	protected List<HeaderField> list = new ArrayList<>();

	protected Map<String, List<IndexAndHeader>> map = new HashMap<>();

	protected int addCount;

	public HeaderTable(boolean dynamic, int startIndex) {
		this.dynamic = dynamic;
		this.startIndex = startIndex;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int size() {
		return size;
	}

	public int maxIndex() {
		return startIndex + list.size() - 1;
	}

	protected HeaderField header(int index) {
		var i = index - startIndex;
		return i >= 0 && i < list.size() ? list.get(i) : null;
	}

	protected List<IndexAndHeader> headers(String name) {
		return map.get(name);
	}

	public void add(HeaderField header) {
		var s = size + size(header);
		if (maxSize >= 0)
			while (s > maxSize && !list.isEmpty()) {
				var h = list.removeLast();
				map.get(h.name()).removeIf(x -> x.header().equals(h));
				s -= size(h);
			}
		if (maxSize < 0 || s <= maxSize) {
			if (dynamic)
				list.add(0, header);
			else
				list.add(header);
			map.compute(header.name(), (_, v) -> {
				if (v == null)
					v = new ArrayList<>();
				v.add(new IndexAndHeader(addCount, header));
				return v;
			});
			addCount++;
			size = s;
		}
	}

	@Override
	public String toString() {
		return "[list=" + list + ",size=" + size + "]";
	}

	protected class IndexAndHeader {

		private final int previousAddCount;

		private final HeaderField header;

		public IndexAndHeader(int previousAddCount, HeaderField header) {
			this.previousAddCount = previousAddCount;
			this.header = header;
		}

		protected int index() {
			return dynamic ? startIndex - 1 + addCount - previousAddCount : startIndex + previousAddCount;
		}

		protected HeaderField header() {
			return header;
		}
	}
}
