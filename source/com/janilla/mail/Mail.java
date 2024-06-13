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
package com.janilla.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.net.Net;
import com.janilla.smtp.CommandSmtpRequest;
import com.janilla.smtp.DataSmtpRequest;
import com.janilla.smtp.MimePartWriter;
import com.janilla.smtp.ReplySmtpResponse;
import com.janilla.smtp.SmtpClient;
import com.janilla.smtp.SmtpRequest;
import com.janilla.smtp.SmtpServer;
import com.janilla.smtp.StubHandler;

public class Mail {

	public static void main(String[] args) {
		var s = new SmtpServer();
		{
			var k = Path.of(System.getProperty("user.home"))
					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
			var p = "passphrase".toCharArray();
			var x = Net.getSSLContext(k, p);
			s.setSslContext(x);
		}
		s.setHandler(new StubHandler());
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

		var m = new Mail();
		m.setAddress(s.getAddress());
//		m.setAddress(new InetSocketAddress("smtp.example.com", 465));
		{
			var k = Path.of(System.getProperty("user.home"))
					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
			var p = "passphrase".toCharArray();
			var x = Net.getSSLContext(k, p);
			m.setSslContext(x);
		}
//		try {
//			var x = SSLContext.getInstance("TLSv1.3");
//			x.init(null, null, null);
//			m.setSslContext(x);
//		} catch (GeneralSecurityException e) {
//			throw new RuntimeException(e);
//		}
		try {
			m.setHostname(InetAddress.getLocalHost().getCanonicalHostName());
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		m.setUsername("john.doe@example.com");
		m.setPassword("**********");
		m.setFrom("john.doe@example.com");
		m.setTo("jane.doe@example.com");
		m.setSubject("Testing Subject");
		m.setMessage("This is message body");
		m.setHtmlMessage("This is my <b style='color:red;'>bold-red email</b> using JavaMailer");
		m.setAttachments(List.of(
				Map.entry("attachment.txt", () -> new ByteArrayInputStream("sample attachment content".getBytes())),
				Map.entry("a.png", () -> {
					try {
						return Files.newInputStream(Path.of(System.getProperty("user.home")).resolve("Pictures/a.png"));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				})));
		try {
			m.send();
		} finally {
			s.stop();
		}
	}

	InetSocketAddress address;

	SSLContext sslContext;

	String hostname;

	String username;

	String password;

	String from;

	String to;

	String subject;

	String message;

	String htmlMessage;

	List<Map.Entry<String, Supplier<InputStream>>> attachments;

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

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getHtmlMessage() {
		return htmlMessage;
	}

	public void setHtmlMessage(String htmlMessage) {
		this.htmlMessage = htmlMessage;
	}

	public List<Map.Entry<String, Supplier<InputStream>>> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Map.Entry<String, Supplier<InputStream>>> attachments) {
		this.attachments = attachments;
	}

	public void send() {
		try (var c = new SmtpClient()) {
			c.setAddress(address);
			c.setSslContext(sslContext);
			var u = c.query(e -> {
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);

			u = c.query(e -> {
				try (var crq = (CommandSmtpRequest) e.getRequest()) {
					crq.writeLine("EHLO " + hostname);
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
					crq.writeLine(Base64.getEncoder().encodeToString(username.getBytes()));
				}
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);

			u = c.query(e -> {
				try (var crq = (CommandSmtpRequest) e.getRequest()) {
					crq.writeLine(Base64.getEncoder().encodeToString(password.getBytes()));
				}
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);

			u = c.query(e -> {
				try (var crq = (CommandSmtpRequest) e.getRequest()) {
					crq.writeLine("MAIL FROM:<" + from + ">");
				}
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);

			u = c.query(e -> {
				try (var crq = (CommandSmtpRequest) e.getRequest()) {
					crq.writeLine("RCPT TO:<" + to + ">");
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

			c.connection().setRequestType(SmtpRequest.Type.DATA);

			u = c.query(e -> {
				try (var drq = (DataSmtpRequest) e.getRequest()) {
					drq.writeHeader("Date: " + OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
					drq.writeHeader("From: " + from);
					drq.writeHeader("To: " + to);
					drq.writeHeader("Message-ID: <" + ThreadLocalRandom.current().ints(25, '0', '9' + 1)
							.mapToObj(Character::toString).collect(Collectors.joining("")) + "@" + hostname + ">");
					drq.writeHeader("Subject: " + subject);
					drq.writeHeader("MIME-Version: 1.0");
					var b = "----=_Part_" + ThreadLocalRandom.current().ints(24, '0', '9' + 1)
							.mapToObj(Character::toString).collect(Collectors.joining());
					drq.writeHeader("Content-Type: multipart/mixed; boundary=\"" + b + "\"");
					var db = (OutputStream) drq.getBody();

					if (message != null && !message.isEmpty())
						try (var pw = new MimePartWriter(db)) {
							pw.writeBoundary(b);
							pw.writeHeader("Content-Type: text/plain; charset=us-ascii");
							pw.writeHeader("Content-Transfer-Encoding: 7bit");
							try {
								((OutputStream) pw.getBody()).write(message.getBytes());
							} catch (IOException f) {
								throw new UncheckedIOException(f);
							}
						}

					if (htmlMessage != null && !htmlMessage.isEmpty())
						try (var pw = new MimePartWriter(db)) {
							pw.writeBoundary(b);
							pw.writeHeader("Content-Type: text/html; charset=utf-8");
							pw.writeHeader("Content-Transfer-Encoding: 7bit");
							try {
								((OutputStream) pw.getBody()).write(htmlMessage.getBytes());
							} catch (IOException f) {
								throw new UncheckedIOException(f);
							}
						}

					if (attachments != null)
						for (var a : attachments)
							try (var pw = new MimePartWriter(db)) {
								pw.writeBoundary(b);
								var n = a.getKey();
								String t;
								{
									var i = n.lastIndexOf('.');
									var ex = i >= 0 ? n.substring(i + 1).toLowerCase() : null;
									t = ex != null ? Map.of("txt", "text/plain", "png", "image/png").get(ex) : null;
								}
								switch (t) {
								case "text/plain":
									pw.writeHeader("Content-Type: " + t + "; charset=us-ascii; name=" + n);
									pw.writeHeader("Content-Transfer-Encoding: 7bit");
									pw.writeHeader("Content-Disposition: attachment; filename=" + n);
									try (var i = a.getValue().get()) {
										i.transferTo((OutputStream) pw.getBody());
									} catch (IOException f) {
										throw new UncheckedIOException(f);
									}
									break;
								case "image/png":
									pw.writeHeader("Content-Type: " + t + "; name=" + n);
									pw.writeHeader("Content-Transfer-Encoding: base64");
									pw.writeHeader("Content-Disposition: attachment; filename=" + n);
									try (var i = a.getValue().get();
											var o = Base64.getMimeEncoder().wrap((OutputStream) pw.getBody())) {
										i.transferTo(o);
									} catch (IOException f) {
										throw new UncheckedIOException(f);
									}
									break;
								default:
									throw new RuntimeException();
								}
							}

					try {
						db.write((b + "--").getBytes());
						db.write("\r\n.\r\n".getBytes());
					} catch (IOException f) {
						throw new UncheckedIOException(f);
					}
				}
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);

			c.connection().setRequestType(SmtpRequest.Type.COMMAND);

			u = c.query(e -> {
				try (var crq = (CommandSmtpRequest) e.getRequest()) {
					crq.writeLine("QUIT");
				}
				var rrs = (ReplySmtpResponse) e.getResponse();
				return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
			});
			System.out.println(u);
		}
	}
}
