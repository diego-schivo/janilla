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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.janilla.io.IO;
import com.janilla.io.LengthReadableByteChannel;
import com.janilla.util.EntryList;

public class HttpMessageReadableByteChannel extends HttpBufferedReadableByteChannel {

	public static void main(String[] args) throws Exception {
		var i = new ByteArrayInputStream("""
				GET /foo HTTP/1.1\r
				Content-Length: 0\r
				\r
				POST /bar HTTP/1.1\r
				Content-Length: 3\r
				\r
				baz
				HTTP/1.1 200 OK\r
				Transfer-Encoding: chunked\r
				\r
				A\r
				foobarbazq\r
				2\r
				ux\r
				0\r
				\r
				""".getBytes());
		try (var c = new HttpMessageReadableByteChannel(Channels.newChannel(i))) {

			try (var r = c.readRequest()) {
				var t = r.getMethod().name() + " " + r.getURI();
				System.out.println(t);
				assert t.equals("GET /foo") : t;

				var h = r.getHeaders();
				System.out.println(h);
				assert Objects.equals(h, List.of(Map.entry("Content-Length", "0"))) : h;
			}

			try (var r = c.readRequest()) {
				var t = r.getMethod().name() + " " + r.getURI();
				System.out.println(t);
				assert t.equals("POST /bar") : t;

				var b = (ReadableByteChannel) r.getBody();
				t = new String(IO.readAllBytes(b));
				System.out.println(t);
				assert t.equals("baz") : t;
			}

			try (var s = c.readResponse()) {
				var t = s.getStatus().code() + " " + s.getStatus().text();
				System.out.println(t);
				assert t.equals("200 OK") : t;

				var b = (ReadableByteChannel) s.getBody();
				t = new String(IO.readAllBytes(b));
				System.out.println(t);
				assert t.equals("foobarbazqux") : t;
			}
		}
	}

	protected static Pattern spaces = Pattern.compile(" +");

	protected long maxMessageLength;

	protected Message message;

	public HttpMessageReadableByteChannel(ReadableByteChannel channel) {
		this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY), -1);
	}

	public HttpMessageReadableByteChannel(ReadableByteChannel channel, ByteBuffer buffer, long maxMessageLength) {
		super(channel, buffer);
		this.maxMessageLength = maxMessageLength;
	}

	public HttpRequest readRequest() {

//		System.out.println(Thread.currentThread().getName() + " HttpMessageReadableByteChannel.readRequest "
//				+ LocalDateTime.now());

		var r = new Request();
		message = r;
		return r;
	}

	public HttpResponse readResponse() {
		var r = new Response();
		message = r;
		return r;
	}

	@Override
	protected int readBuffer() {
		var n = super.readBuffer();
		message.count += n;
		if (maxMessageLength >= 0 && message.count > maxMessageLength)
			throw new RuntimeException("message.count=" + message.count);
		return n;
	}

	protected abstract class Message implements HttpMessage {

		protected Channel body;

		protected long count;

		protected EntryList<String, String> headers;

		protected String startLine;

		protected int state;

		@Override
		public EntryList<String, String> getHeaders() {
			advance(2);
			return headers;
		}

		@Override
		public Channel getBody() {
			advance(3);
			return body;
		}

		@Override
		public void close() {
			Channel b;
			try {
				b = getBody();
			} catch (UncheckedIOException e) {
				b = null;
			}
			if (b != null)
				try {
					b.close();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
		}

		protected void advance(int target) {
			if (target >= 1 && state < 1) {
				startLine = readLine();
				if (startLine == null) {
					state = 3;
					throw new NullPointerException("startLine");
				}
				state = 1;
			}

			if (target >= 2 && state < 2) {
				headers = new EntryList<>();
				for (;;) {
					var l = readLine();
					if (l == null || l.isEmpty())
						break;
					var i = l.indexOf(':');
					var k = l.substring(0, i).trim();
					var v = l.substring(i + 1).trim();
					headers.add(k, v);
				}
				state = 2;
			}

			if (target >= 3 && state < 3) {
				var s = headers.get("Transfer-Encoding");
				if (s != null)
					switch (s) {
					case "chunked":
						body = new ChunkedReadableByteChannel(HttpMessageReadableByteChannel.this,
								ByteBuffer.allocate(buffer.capacity())) {

							@Override
							public void close() throws IOException {
								if (ended)
									return;
								var b = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
								var m = System.currentTimeMillis();
								for (;;) {
									if (!b.hasRemaining())
										b.clear();

									var n = read(b);

									if (ended || n < 0)
										break;
									else if (n == 0) {
										if (System.currentTimeMillis() - m < 1000)
											try {
												Thread.sleep(10);
											} catch (InterruptedException e) {
												throw new RuntimeException(e);
											}
										else
											throw new IOException();
									} else
										m = System.currentTimeMillis();
								}
							}
						};
						break;
					default:
						throw new RuntimeException();
					}
				else {
					s = headers.get("Content-Length");
					var l = s != null ? Integer.parseInt(s) : 0;
					body = new LengthReadableByteChannel(HttpMessageReadableByteChannel.this, l) {

						@Override
						public void close() throws IOException {
							if (length == 0 || ended)
								return;
							var b = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
							var m = System.currentTimeMillis();
							for (;;) {
								if (!b.hasRemaining())
									b.clear();

								var n = read(b);

								if (ended || n < 0)
									break;
								else if (n == 0) {
									if (System.currentTimeMillis() - m < 1000)
										try {
											Thread.sleep(10);
										} catch (InterruptedException e) {
											throw new RuntimeException(e);
										}
									else
										throw new IOException();
								} else
									m = System.currentTimeMillis();
							}
						}
					};
				}
				state = 3;
			}
		}
	}

	protected class Request extends Message implements HttpRequest {

		protected Method method;

		protected URI uri;

		@Override
		public Method getMethod() {
			advance(1);
			return method;
		}

		@Override
		public void setMethod(Method value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public URI getURI() {
			advance(1);
			return uri;
		}

		@Override
		public void setURI(URI value) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void advance(int target) {
			var x = target >= 1 && state < 1;
			super.advance(target);
			if (x) {
				var s = spaces.split(startLine.strip(), 3);
				method = new Method(s[0]);
				uri = URI.create(s[1]);
			}
		}
	}

	protected class Response extends Message implements HttpResponse {

		protected Status status;

		@Override
		public Status getStatus() {
			advance(1);
			return status;
		}

		@Override
		public void setStatus(Status value) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void advance(int target) {
			var x = target >= 1 && state < 1;
			super.advance(target);
			if (x) {
				var s = spaces.split(startLine.strip(), 3);
				var c = Integer.parseInt(s[1]);
				status = s.length >= 3 ? new Status(c, s[2]) : Status.of(c);
			}
		}
	}
}
