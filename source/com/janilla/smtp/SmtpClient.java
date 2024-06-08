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
package com.janilla.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.net.Net;

public class SmtpClient implements AutoCloseable {

	public static void main(String[] args) {
		var s = new SmtpServer();
		{
			var k = Path.of(System.getProperty("user.home"))
					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
			var p = "passphrase".toCharArray();
			var x = Net.getSSLContext(k, p);
			s.setSslContext(x);
		}
		new Thread(s::run, "Server").start();

		synchronized (s) {
			while (s.getAddress() == null) {
				try {
					s.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		try (var c = new SmtpClient()) {
			c.setAddress(s.getAddress());
			{
				var k = Path.of(System.getProperty("user.home"))
						.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
				var p = "passphrase".toCharArray();
				var x = Net.getSSLContext(k, p);
				c.setSslContext(x);
			}
			try {
				var u = c.query(e -> {
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("EHLO example.lan");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("AUTH LOGIN");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("********************************");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("****************");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("MAIL FROM:<foo@example.com>");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("RCPT TO:<bar@example.com>");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("DATA");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				c.connection().setState(SmtpConnection.State.DATA);

				u = c.query(e -> {
					try (var crq = (DataSmtpRequest) e.getRequest()) {
						crq.writeHeader("Date: Wed, 5 Jun 2024 15:24:07 +0200 (CEST)");
						crq.writeHeader("From: diego.schivo@janilla.com");
						crq.writeHeader("To: diego.schivo@gmail.com");
						crq.writeHeader("Message-ID: <497359413.1.1717593847390@asuslaptop.lan>");
						crq.writeHeader("Subject: Testing Subject");
						crq.writeHeader("MIME-Version: 1.0");
						crq.writeHeader(
								"Content-Type: multipart/mixed; boundary=\"----=_Part_0_54495403.1717593847312\"");

						var b = (OutputStream) crq.getBody();
						try (var pw = new MimePartWriter(b)) {
							pw.writeBoundary("----=_Part_0_54495403.1717593847312");
							pw.writeHeader("Content-Type: text/plain; charset=us-ascii");
							pw.writeHeader("Content-Transfer-Encoding: 7bit");
							try {
								((OutputStream) pw.getBody()).write("This is message body".getBytes());
							} catch (IOException f) {
								throw new UncheckedIOException(f);
							}
						}

						try (var pw = new MimePartWriter(b)) {
							pw.writeBoundary("----=_Part_0_54495403.1717593847312");
							pw.writeHeader("Content-Type: text/html; charset=utf-8");
							pw.writeHeader("Content-Transfer-Encoding: 7bit");
							try {
								((OutputStream) pw.getBody())
										.write("This is my <b style='color:red;'>bold-red email</b> using JavaMailer"
												.getBytes());
							} catch (IOException f) {
								throw new UncheckedIOException(f);
							}
						}

						try (var pw = new MimePartWriter(b)) {
							pw.writeBoundary("----=_Part_0_54495403.1717593847312");
							pw.writeHeader("Content-Type: text/plain; charset=us-ascii; name=attachment.txt");
							pw.writeHeader("Content-Transfer-Encoding: 7bit");
							pw.writeHeader("Content-Disposition: attachment; filename=attachment.txt");
							try {
								((OutputStream) pw.getBody()).write("sample attachment content".getBytes());
							} catch (IOException f) {
								throw new UncheckedIOException(f);
							}
						}

						try {
							b.write(".\r\n".getBytes());
						} catch (IOException f) {
							throw new UncheckedIOException(f);
						}
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				c.connection().setState(SmtpConnection.State.COMMAND);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("QUIT");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			s.stop();
		}
	}

	private InetSocketAddress address;

	private SSLContext sslContext;

	protected SmtpConnection connection;

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public SmtpConnection connection() {
		return connection;
	}

	public <T> T query(Function<SmtpExchange, T> handler) {
		if (connection == null) {
			try {
				var sc = SocketChannel.open();
				sc.configureBlocking(true);
				sc.connect(address);
				var e = sslContext.createSSLEngine(address.getHostName(), address.getPort());
				e.setUseClientMode(true);
				connection = new SmtpConnection.Builder().channel(sc).sslEngine(e).build();
				connection.setUseClientMode(true);
				connection.setState(SmtpConnection.State.NEW);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		try (var rq = connection.newRequest(); var rs = connection.newResponse()) {
			var e = buildExchange(rq, rs);
			return handler.apply(e);
		}
	}

	@Override
	public void close() {
		if (connection != null)
			connection.close();
	}

	protected SmtpExchange buildExchange(SmtpRequest request, SmtpResponse response) {
		var e = new SmtpExchange();
		e.setRequest(request);
		e.setResponse(response);
		return e;
	}
}
