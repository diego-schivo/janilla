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
package com.janilla.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import javax.net.ssl.SSLEngine;

import com.janilla.io.FilterByteChannel;
import com.janilla.io.IO;
import com.janilla.io.TextReadableByteChannel;
import com.janilla.io.TextWritableByteChannel;
import com.janilla.net.SSLByteChannel;

public class SmtpConnection implements AutoCloseable {

	protected SocketChannel socketChannel;

	protected SSLByteChannel sslChannel;

	protected InputChannel inputChannel;

	protected OutputChannel outputChannel;

	protected boolean useClientMode;

	protected SmtpRequest.Type requestType;

	public SocketChannel socketChannel() {
		return socketChannel;
	}

	public void setUseClientMode(boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public SmtpRequest.Type getRequestType() {
		return requestType;
	}

	public void setRequestType(SmtpRequest.Type requestType) {
		this.requestType = requestType;
	}

	public SmtpRequest newRequest() {
		return useClientMode ? outputChannel.newRequest() : inputChannel.newRequest();
	}

	public SmtpResponse newResponse() {
		return useClientMode ? inputChannel.newResponse() : outputChannel.newResponse();
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

		protected SmtpConnection connection;

		protected SocketChannel channel;

		protected SSLEngine sslEngine;

		protected int bufferCapacity = IO.DEFAULT_BUFFER_CAPACITY;

		public Builder connection(SmtpConnection connection) {
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

		public SmtpConnection build() {
			var c = connection != null ? connection : new SmtpConnection();
			c.socketChannel = channel;
			c.sslChannel = new SSLByteChannel(channel, sslEngine);
			var ch = new FilterByteChannel(c.sslChannel) {

				@Override
				public int read(ByteBuffer dst) throws IOException {
					var n = super.read(dst);

					if (n > 0)
						System.out.println(
								"<<< " + new String(dst.array(), dst.position() - n, n).replace("\n", "\n<<< "));

//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}

					return n;
				}

				@Override
				public int write(ByteBuffer src) throws IOException {
					var n = super.write(src);

					if (n > 0)
						System.out.println(
								">>> " + new String(src.array(), src.position() - n, n).replace("\n", "\n>>> "));

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

		public SmtpRequest newRequest() {
			var rq = switch (requestType) {
			case EMPTY -> new EmptyRequest();
			case COMMAND -> new CommandRequest();
			case DATA -> new DataRequest();
			};
			return rq;
		}

		public SmtpResponse newResponse() {
			return new ReplyResponse();
		}

		protected class EmptyRequest implements SmtpRequest {

			@Override
			public void close() {
			}
		}

		protected class CommandRequest implements CommandSmtpRequest {

			@Override
			public String readLine() {
				return InputChannel.this.readLine();
			}

			@Override
			public void writeLine(String line) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() {
			}
		}

		protected class DataRequest implements DataSmtpRequest {

			@Override
			public String readHeader() {
				var l = InputChannel.this.readLine();
				return l != null && !l.isEmpty() ? l : null;
			}

			@Override
			public void writeHeader(String line) {
				throw new UnsupportedOperationException();
			}

			@Override
			public InputStream getBody() {
				return new InputStream() {

					int state;

					@Override
					public int read() throws IOException {
						if (state == 5)
							return -1;
						var b = readByte();
						state = switch (state) {
						case 0 -> b == '\r' ? 1 : 0;
						case 1 -> b == '\n' ? 2 : b == '\r' ? 1 : 0;
						case 2 -> b == '.' ? 3 : b == '\r' ? 1 : 0;
						case 3 -> b == '\r' ? 4 : b == '\r' ? 1 : 0;
						case 4 -> b == '\n' ? 5 : b == '\r' ? 1 : 0;
						default -> throw new IllegalStateException();
						};
						return b;
					}
				};
			}

			@Override
			public void close() {
			}
		}

		protected class ReplyResponse implements ReplySmtpResponse {

			String previous;

			@Override
			public String readLine() {
				if (previous != null && previous.charAt(3) == ' ')
					return null;
				var l = InputChannel.this.readLine();
				previous = l;
				return l;
			}

			@Override
			public void writeLine(String line) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() {
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

		public SmtpRequest newRequest() {
			var rq = switch (requestType) {
			case COMMAND -> new CommandRequest();
			case DATA -> new DataRequest();
			default -> throw new IllegalArgumentException();
			};
			return rq;
		}

		public SmtpResponse newResponse() {
			return new ReplyResponse();
		}

		protected class CommandRequest implements CommandSmtpRequest {

			@Override
			public String readLine() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void writeLine(String line) {
				try {
					OutputChannel.this.writeLine(line);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

			}

			@Override
			public void close() {
				try {
					flush();
				} catch (IOException e) {
				}
			}
		}

		protected class DataRequest implements DataSmtpRequest {

			@Override
			public String readHeader() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void writeHeader(String line) {
				try {
					OutputChannel.this.writeLine(line);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			@Override
			public OutputStream getBody() {
				try {
					OutputChannel.this.writeLine("");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return new OutputStream() {

					@Override
					public void write(int b) throws IOException {
						writeByte((byte) b);
					}
				};
			}

			@Override
			public void close() {
				try {
					flush();
				} catch (IOException e) {
				}
			}
		}

		protected class ReplyResponse implements ReplySmtpResponse {

			String previous;

			@Override
			public String readLine() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void writeLine(String line) {
				if (previous != null)
					try {
						OutputChannel.this.writeLine(previous.substring(0, 3) + "-" + previous.substring(4));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				previous = line;
			}

			@Override
			public void close() {
				try {
					if (previous != null)
						OutputChannel.this.writeLine(previous.substring(0, 3) + " " + previous.substring(4));
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
