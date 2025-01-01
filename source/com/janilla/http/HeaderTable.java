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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderTable {

	static HeaderTable STATIC;

	static {
		STATIC = new HeaderTable();
		STATIC.setStartIndex(1);
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
			return new HeaderField(i >= 0 ? x.substring(0, i).trim() : x, i >= 0 ? x.substring(i + 1).trim() : null);
		}).forEach(STATIC::add);
	}

	static int size(HeaderField header) {
		return header.name().length() + (header.value() != null ? header.value().length() : 0) + 32;
	}

	boolean dynamic;

	int startIndex;

	int maxSize = -1;

	int size;

	List<HeaderField> list = new ArrayList<>();

	Map<String, List<IndexAndHeader>> map = new HashMap<>();

	int addCount;

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public void setStartIndex(int startIndex) {
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

	HeaderField header(int index) {
		var i = index - startIndex;
		return i >= 0 && i < list.size() ? list.get(i) : null;
	}

	List<IndexAndHeader> headers(String name) {
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

	class IndexAndHeader {

		private int previousAddCount;

		private HeaderField header;

		private IndexAndHeader(int previousAddCount, HeaderField header) {
			this.previousAddCount = previousAddCount;
			this.header = header;
		}

		int index() {
			return dynamic ? startIndex - 1 + addCount - previousAddCount : startIndex + previousAddCount;
		}

		HeaderField header() {
			return header;
		}
	}
}
