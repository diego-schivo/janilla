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
package com.janilla.hpack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HeaderTable extends ArrayList<Header> {

	private static final long serialVersionUID = -2826276932226096535L;

	static HeaderTable staticTable;

	static {
		staticTable = new HeaderTable();
		staticTable.startIndex = 1;
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
			return new Header(i >= 0 ? x.substring(0, i).trim() : x, i >= 0 ? x.substring(i + 1).trim() : null);
		}).forEach(staticTable::add);
	}

	boolean dynamic;

	int startIndex;

	Map<String, List<IndexAndHeader>> map = new HashMap<>();

	Header header(int index) {
		var i = index - startIndex;
		return i >= 0 && i < size() ? get(dynamic ? size() - 1 - i : i) : null;
	}

	List<IndexAndHeader> headers(String name) {
		return map.get(name);
	}

	@Override
	public boolean add(Header e) {
		var a = super.add(e);
		map.compute(e.name(), (k, v) -> {
			if (v == null)
				v = new ArrayList<>();
			v.add(new IndexAndHeader(size() - 1, e));
			return v;
		});
		return a;
	}

	class IndexAndHeader {

		private int index;

		private Header header;

		private IndexAndHeader(int index, Header header) {
			this.index = index;
			this.header = header;
		}

		int index() {
			return startIndex + (dynamic ? size() - 1 - index : index);
		}

		Header header() {
			return header;
		}
	}
}
