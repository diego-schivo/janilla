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

import java.util.Map;
import java.util.stream.Collectors;

public interface HttpResponse extends HttpMessage {

	Status getStatus();

	void setStatus(Status status);

	public record Status(int code, String text) {

		public static Map<Integer, String> texts = """
				100	Continue
				101	Switching Protocols
				102	Processing
				103	Early Hints
				200	OK
				201	Created
				202	Accepted
				203	Non-Authoritative Information
				204	No Content
				205	Reset Content
				206	Partial Content
				207	Multi-Status
				208	Already Reported
				226	IM Used
				300	Multiple Choices
				301	Moved Permanently
				302	Found
				303	See Other
				304	Not Modified
				305	Use Proxy Deprecated
				306	unused
				307	Temporary Redirect
				308	Permanent Redirect
				400	Bad Request
				401	Unauthorized
				402	Payment Required Experimental
				403	Forbidden
				404	Not Found
				405	Method Not Allowed
				406	Not Acceptable
				407	Proxy Authentication Required
				408	Request Timeout
				409	Conflict
				410	Gone
				411	Length Required
				412	Precondition Failed
				413	Payload Too Large
				414	URI Too Long
				415	Unsupported Media Type
				416	Range Not Satisfiable
				417	Expectation Failed
				418	I'm a teapot
				421	Misdirected Request
				422	Unprocessable Content
				423	Locked
				424	Failed Dependency
				425	Too Early Experimental
				426	Upgrade Required
				428	Precondition Required
				429	Too Many Requests
				431	Request Header Fields Too Large
				451	Unavailable For Legal Reasons
				500	Internal Server Error
				501	Not Implemented
				502	Bad Gateway
				503	Service Unavailable
				504	Gateway Timeout
				505	HTTP Version Not Supported
				506	Variant Also Negotiates
				507	Insufficient Storage
				508	Loop Detected
				510	Not Extended
				511	Network Authentication Required""".lines().collect(Collectors
				.toMap(x -> Integer.parseInt(x.substring(0, x.indexOf('\t'))), x -> x.substring(x.indexOf('\t') + 1)));

		public static Status of(int code) {
			return new Status(code, texts.get(code));
		}
	}

	class Default extends HttpMessage.Default implements HttpResponse {

		protected Status status;

		@Override
		public Status getStatus() {
			if (state == 0)
				getStartLine();
			return status;
		}

		@Override
		public void setStatus(Status status) {
			this.status = status;
			if (status != null)
				setStartLine("HTTP/1.1 " + status.code() + " " + status.text());
		}

		@Override
		public String getStartLine() {
			var s = state;
			var l = super.getStartLine();
			if (s == 0) {
				var ss = l != null ? l.split(" ", 3) : null;
				status = ss != null && ss.length > 2 ? new Status(Integer.parseInt(ss[1].trim()), ss[2].trim()) : null;
			}
			return l;
		}
	}
}
