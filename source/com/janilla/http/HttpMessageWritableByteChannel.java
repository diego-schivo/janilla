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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import com.janilla.http.HttpResponse.Status;
import com.janilla.io.BufferedWritableByteChannel;
import com.janilla.io.IO;
import com.janilla.io.LengthWritableByteChannel;
import com.janilla.util.EntryList;

public class HttpMessageWritableByteChannel extends HttpBufferedWritableByteChannel {

	public static void main(String[] args) throws IOException {
		var o = new ByteArrayOutputStream();
		try (var c = new HttpMessageWritableByteChannel(Channels.newChannel(o), ByteBuffer.allocate(10))) {

			try (var s = c.writeResponse()) {
				s.setStatus(new Status(404, "Not Found"));
			}
			var t = o.toString();
			System.out.println(t);
			assert Objects.equals(t, """
					HTTP/1.1 404 Not Found\r
					Content-Length: 0\r
					\r
					""") : t;
			o.reset();

			try (var s = c.writeResponse()) {
				s.setStatus(new Status(200, "OK"));
				var b = (WritableByteChannel) s.getBody();
				IO.write("foo".getBytes(), b);
			}

			t = o.toString();
			System.out.println(t);
			assert Objects.equals(t, """
					HTTP/1.1 200 OK\r
					Content-Length: 3\r
					\r
					foo""") : t;
			o.reset();

			try (var s = c.writeResponse()) {
				s.setStatus(new Status(200, "OK"));
				var b = (WritableByteChannel) s.getBody();
				IO.write("foobarbazqux".getBytes(), b);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			t = o.toString();
			System.out.println(t);
			assert Objects.equals(t, """
					HTTP/1.1 200 OK\r
					Transfer-Encoding: chunked\r
					\r
					A\r
					foobarbazq\r
					2\r
					ux\r
					0\r
					\r
					""") : t;
			o.reset();

			try (var s = c.writeResponse()) {
				s.setStatus(new Status(200, "OK"));
				s.getHeaders().add("Content-Length", "12");
				var b = (WritableByteChannel) s.getBody();
				IO.write("foobarbazqux".getBytes(), b);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			t = o.toString();
			System.out.println(t);
			assert Objects.equals(t, """
					HTTP/1.1 200 OK\r
					Content-Length: 12\r
					\r
					foobarbazqux""") : t;
			o.reset();

			try (var s = c.writeResponse()) {
				s.setStatus(new Status(200, "OK"));
				s.getHeaders().add("Transfer-Encoding", "chunked");
				var b = (WritableByteChannel) s.getBody();
				IO.write("foo".getBytes(), b);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			t = o.toString();
			System.out.println(t);
			assert Objects.equals(t, """
					HTTP/1.1 200 OK\r
					Transfer-Encoding: chunked\r
					\r
					3\r
					foo\r
					0\r
					\r
					""") : t;
		}
	}

	protected Message message;

	public HttpMessageWritableByteChannel(WritableByteChannel channel) {
		this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
	}

	public HttpMessageWritableByteChannel(WritableByteChannel channel, ByteBuffer buffer) {
		super(channel, buffer);
	}

	public HttpRequest writeRequest() {
		var r = new Request();
		message = r;
		return r;
	}

	public HttpResponse writeResponse() {
		var r = new Response();
		message = r;
		return r;
	}

	protected abstract class Message implements HttpMessage {

		protected EntryList<String, String> headers;

		protected WritableByteChannel body;

		protected int state;

		protected abstract String startLine();

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
		public void close() throws IOException {
			if (state > 0)
				getBody().close();
		}

		protected void advance(int target) {
			if (target >= 1 && state < 1)
				state = 1;

			if (target >= 2 && state < 2) {
				headers = new EntryList<>();
				state = 2;
			}

			if (target >= 3 && state < 3) {
				body = new Body();
				state = 3;
			}

			if (target >= 4 && state < 4) {
				try {
					writeLine(startLine());
					for (var h : headers)
						writeLine(h.getKey() + ": " + h.getValue());
					writeLine("");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				state = 4;
			}
		}

		protected class Body extends BufferedWritableByteChannel {

			public Body() {
				super(HttpMessageWritableByteChannel.this,
						ByteBuffer.allocate(HttpMessageWritableByteChannel.this.buffer.capacity()));
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
//				if (src.hasRemaining())
//					System.out.println(
//							"1>>> " + new String(src.array(), src.position(), src.remaining()).replace("\n", "\n1>>> "));

				var n = 0;

				if (body == this)
					n += super.write(src);

				if (body != this) {
					transferBuffer();
					n += body.write(src);
				}

//				if (n > 0)
//					System.out.println("2>>> "
//							+ new String(src.array(), src.position() - n, src.position()).replace("\n", "\n2>>> "));

				return n;
			}

			@Override
			public void close() throws IOException {
				if (state == 3)
					switchBody();

				if (body == this) {
					flush();
					try {
						HttpMessageWritableByteChannel.this.flush();
					} catch (IOException e) {
					}
				} else {
					transferBuffer();
					body.close();
				}
			}

			@Override
			protected int writeBuffer(boolean all, int max) throws IOException {
				if (state == 3)
					switchBody();

				return body == this ? super.writeBuffer(all, max) : 0;
			}

			protected void switchBody() {
				var s = headers.get("Content-Length");
				if (s != null) {
					var n = Integer.parseInt(s);
					advance(4);
					body = new LengthWritableByteChannel(HttpMessageWritableByteChannel.this, n) {

						@Override
						public void close() throws IOException {
							flush();
							HttpMessageWritableByteChannel.this.flush();
						}
					};
					return;
				}

				s = headers.get("Transfer-Encoding");
				if (s != null)
					switch (s) {
					case "chunked":
						advance(4);
						body = new ChunkedWritableByteChannel(HttpMessageWritableByteChannel.this,
								ByteBuffer.allocate(buffer.capacity())) {

							@Override
							public void close() throws IOException {
								flush();
								HttpMessageWritableByteChannel.this.flush();
							}
						};
						return;
					default:
						throw new RuntimeException();
					}

				if (buffer.hasRemaining()) {
					var p = buffer.position();
//					System.out.println(Thread.currentThread().getName() + " p=" + p);
					headers.add("Content-Length", String.valueOf(p));
					advance(4);
					return;
				}

				headers.add("Transfer-Encoding", "chunked");
				advance(4);
				body = new ChunkedWritableByteChannel(HttpMessageWritableByteChannel.this,
						ByteBuffer.allocate(buffer.capacity())) {

					@Override
					public void close() throws IOException {
						flush();
						HttpMessageWritableByteChannel.this.flush();
					}
				};
			}

			protected void transferBuffer() throws IOException {
				if (buffer.position() == 0)
					return;

				buffer.flip();
				try {
					do
						body.write(buffer);
					while (buffer.hasRemaining());
				} finally {
					buffer.compact();
				}
			}
		}
	}

	protected class Request extends Message implements HttpRequest {

		protected Method method;

		protected URI uri;

		@Override
		public Method getMethod() {
			return method;
		}

		@Override
		public void setMethod(Method value) {
			advance(1);
			method = value;
		}

		@Override
		public URI getURI() {
			return uri;
		}

		@Override
		public void setURI(URI value) {
			advance(1);
			uri = value;
		}

		@Override
		protected String startLine() {
			return method.name() + " " + uri + " HTTP/1.1";
		}
	}

	protected class Response extends Message implements HttpResponse {

		protected Status status;

		@Override
		public Status getStatus() {
			return status;
		}

		@Override
		public void setStatus(Status value) {
			advance(1);
			status = value;
		}

		@Override
		protected String startLine() {
			return "HTTP/1.1 " + status.code() + " " + status.text();
		}
	}
}
