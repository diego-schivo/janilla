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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;

import com.janilla.io.FilterByteChannel;
import com.janilla.io.IO;
import com.janilla.io.TextReadableByteChannel;
import com.janilla.io.TextWritableByteChannel;
import com.janilla.net.SSLByteChannel;

public class HttpConnection implements AutoCloseable {

	protected SocketChannel socketChannel;

	protected SSLByteChannel sslChannel;

	protected InputChannel inputChannel;

	protected OutputChannel outputChannel;

	protected boolean useClientMode;

	public SocketChannel socketChannel() {
		return socketChannel;
	}

	public void setUseClientMode(boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public HttpMessage.Raw startRequest() {
		return useClientMode ? outputChannel.startMessage() : inputChannel.startMessage();
	}

	public HttpMessage.Raw startResponse() {
		return useClientMode ? inputChannel.startMessage() : outputChannel.startMessage();
	}

	@Override
	public void close() {
		try {
			inputChannel.close();
		} catch (IOException e) {
		}
		try {
			outputChannel.close();
		} catch (IOException e) {
		}
		while (socketChannel.isOpen())
			try {
				socketChannel.close();
			} catch (IOException e) {
			}
	}

	public static class Builder {

		protected HttpConnection connection;

		protected SocketChannel channel;

		protected SSLEngine sslEngine;

		protected int bufferCapacity = IO.DEFAULT_BUFFER_CAPACITY;

		public Builder connection(HttpConnection connection) {
			this.connection = connection;
			return this;
		}

		public Builder channel(SocketChannel channel) {
			this.channel = channel;
			return this;
		}

		public Builder sslEngine(SSLEngine sslEngine) {
			this.sslEngine = sslEngine;
			return this;
		}

		public Builder bufferCapacity(int bufferCapacity) {
			this.bufferCapacity = bufferCapacity;
			return this;
		}

		public HttpConnection build() {
			var c = connection != null ? connection : new HttpConnection();
			c.socketChannel = channel;
			if (sslEngine != null)
				c.sslChannel = new SSLByteChannel(channel, sslEngine);
			var ch = c.sslChannel != null ? c.sslChannel : c.socketChannel;
			ch = new FilterByteChannel(ch) {

				@Override
				public int read(ByteBuffer dst) throws IOException {
					var n = super.read(dst);

//					if (n > 0)
//						System.out.println(
//								"<<< " + new String(dst.array(), dst.position() - n, n).replace("\n", "\n<<< "));

//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}

					return n;
				}

				@Override
				public int write(ByteBuffer src) throws IOException {
					var n = super.write(src);

//					if (n > 0)
//						System.out.println(
//								">>> " + new String(src.array(), src.position() - n, n).replace("\n", "\n>>> "));

//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}

					return n;
				}
			};

			c.inputChannel = c.new InputChannel(ch, ByteBuffer.allocate(bufferCapacity));
			c.outputChannel = c.new OutputChannel(ch, ByteBuffer.allocate(bufferCapacity));
			return c;
		}
	}

	protected class InputChannel extends TextReadableByteChannel {

		public InputChannel(ReadableByteChannel channel) {
			this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
		}

		public InputChannel(ReadableByteChannel channel, ByteBuffer buffer) {
			super(channel, buffer);
		}

		public HttpMessage.Raw startMessage() {
			return new Message();
		}

		protected class Message implements HttpMessage.Raw {

			protected long length;

			protected boolean chunked;

			protected InputStream body;

			@Override
			public String readStartLine() {
				return readLine();
			}

			@Override
			public void writeStartLine(String line) {
				throw new UnsupportedOperationException();
			}

			@Override
			public HttpHeader readHeader() {
				var l = readLine();
				if (l != null && l.isEmpty())
					l = null;
				var i = l != null ? l.indexOf(':') : -1;
				var n = i >= 0 ? l.substring(0, i).trim() : l;
				var v = i >= 0 ? l.substring(i + 1).trim() : null;
				var h = n != null ? new HttpHeader(n, v) : null;

				if (h != null)
					switch (h.name().toLowerCase()) {
					case "content-length":
						length = Long.parseLong(h.value());
						break;
					case "transfer-encoding":
						chunked = Arrays.stream(h.value().split(",")).anyMatch(x -> x.trim().equals("chunked"));
						break;
					}

				return h;
			}

			@Override
			public void writeHeader(HttpHeader header) {
				throw new UnsupportedOperationException();
			}

			@Override
			public InputStream getBody() {
				if (body == null) {
					body = chunked ? new InputStream() {

						int state;

						StringBuilder builder = new StringBuilder();

						long length;

						long count;

						@Override
						public int read() throws IOException {
							int b;
							state = switch (state) {
							case 0 -> {
								b = readByte();
								if (b != '\n') {
									if (builder.length() < 17)
										builder.append((char) b);
									else
										throw new RuntimeException();
									yield 0;
								}
								length = builder.length() >= 2 && builder.charAt(builder.length() - 1) == '\r'
										? Long.parseLong(builder.substring(0, builder.length() - 1), 16)
										: -1;
								if (length < 0)
									throw new RuntimeException();
								builder.setLength(0);
								yield length > 0 ? 1 : 2;
							}
							case 1 -> {
								b = readByte();
								if (++count < length)
									yield 1;
								count = 0;
								yield 2;
							}
							case 2 -> {
								b = readByte();
								if (b != '\n') {
									if (!builder.isEmpty())
										throw new RuntimeException();
									builder.append((char) b);
									yield 2;
								}
								if (builder.isEmpty() || builder.charAt(0) != '\r')
									throw new RuntimeException();
								builder.setLength(0);
								yield length > 0 ? 0 : 3;
							}
							case 3 -> {
								b = -1;
								yield 3;
							}
							default -> throw new IllegalStateException();
							};
							return b;
						}

						@Override
						public void close() throws IOException {
							super.close();
						}
					} : new InputStream() {

						long count;

						@Override
						public int read() throws IOException {
							var b = count < length ? readByte() : -1;
							if (b >= 0)
								count++;
							return b;
						}

						@Override
						public void close() throws IOException {
							while (count < length && read() >= 0)
								;
							super.close();
						}
					};
				}
				return body;
			}

			@Override
			public void close() {
				if (body != null)
					try {
						body.close();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
			}
		}
	}

	protected class OutputChannel extends TextWritableByteChannel {

		public OutputChannel(WritableByteChannel channel) {
			this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
		}

		public OutputChannel(WritableByteChannel channel, ByteBuffer buffer) {
			super(channel, buffer);
		}

		public HttpMessage.Raw startMessage() {
			return new Message();
		}

		protected class Message implements HttpMessage.Raw {

			protected OutputStream body;

			@Override
			public String readStartLine() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void writeStartLine(String line) {
				try {
					writeLine(line);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			@Override
			public HttpHeader readHeader() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void writeHeader(HttpHeader header) {
				try {
					writeLine(header.name() + ": " + header.value());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			@Override
			public OutputStream getBody() {
				if (body == null) {
					try {
						writeLine("");
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					body = new OutputStream() {

						@Override
						public void write(int b) throws IOException {
							writeByte((byte) b);
						}
					};
				}
				return body;
			}

			@Override
			public void close() {
				if (body != null)
					try {
						body.close();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				try {
					flush();
				} catch (IOException e) {
				}
			}
		}
	}
}
