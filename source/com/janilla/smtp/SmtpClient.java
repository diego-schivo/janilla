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
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.net.SslByteChannel;

public class SmtpClient {

	protected final String host;

	protected final int port;

	protected final String username;

	protected final String password;

	public SmtpClient(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	private static final Pattern EMAIL = Pattern.compile("<(.*?)>");

	public void sendMail(OffsetDateTime date, String from, String to, String subject, String message) {
		SSLContext ssl;
		try {
			ssl = SSLContext.getInstance("TLSv1.3");
			ssl.init(null, null, null);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		try (var ch = SocketChannel.open()) {
			var isa = new InetSocketAddress(InetAddress.getByName(host), port);
			ch.connect(isa);
			var se = ssl.createSSLEngine();
			se.setUseClientMode(true);
			try (var sch = new SslByteChannel(ch, se)) {
				var hostname = InetAddress.getLocalHost().getCanonicalHostName();
				var ss = List.of(from, to).stream().map(x -> {
					var m = EMAIL.matcher(x);
					return m.find() ? m.group(1) : x;
				}).toList();
				var dbuf = ByteBuffer.allocateDirect(8 * 1024);
				var charset = Charset.forName("US-ASCII");
				var encoder = charset.newEncoder();
				var decoder = charset.newDecoder();
				for (var s : new String[] { null, "EHLO " + hostname, "AUTH LOGIN",
						Base64.getEncoder().encodeToString(username.getBytes()),
						Base64.getEncoder().encodeToString(password.getBytes()), "MAIL FROM:<" + ss.get(0) + ">",
						"RCPT TO:<" + ss.get(1) + ">", "DATA",
						"Date: " + date.format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\nFrom: " + from + "\nTo: " + to
								+ "\nMessage-ID: <"
								+ ThreadLocalRandom.current().ints(25, '0', '9' + 1).mapToObj(Character::toString)
										.collect(Collectors.joining(""))
								+ "@" + hostname + ">\nSubject: " + subject
								+ "\nMIME-Version: 1.0\nContent-Type: text/plain; charset=us-ascii\nContent-Transfer-Encoding: 7bit\n\n"
								+ message + "\n.",
						"QUIT" }) {
					if (s != null) {
						System.out.println("C: " + s.replace("\n", "\nC: "));
						sch.write(encoder.encode(CharBuffer.wrap(s.replace("\n", "\r\n") + "\r\n")));
					}
					dbuf.clear();
					sch.read(dbuf);
					dbuf.flip();
					var cb = decoder.decode(dbuf);
					System.out.println("\t" + cb.toString().replace("\n", "\n\t"));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//	public void sendMail(OffsetDateTime date, String from, String to, String subject, String message,
//			String htmlMessage) {
//		SSLContext ssl;
//		try {
//			ssl = SSLContext.getInstance("TLSv1.3");
//			ssl.init(null, null, null);
//		} catch (GeneralSecurityException e) {
//			throw new RuntimeException(e);
//		}
//		try (var ch = SocketChannel.open()) {
//			var isa = new InetSocketAddress(InetAddress.getByName(host), port);
//			ch.connect(isa);
//			var se = ssl.createSSLEngine();
//			se.setUseClientMode(true);
//			try (var sch = new SslByteChannel(ch, se)) {
//				var hostname = InetAddress.getLocalHost().getCanonicalHostName();
//				var b = "--=_Part_" + ThreadLocalRandom.current().ints(24, '0', '9' + 1).mapToObj(Character::toString)
//						.collect(Collectors.joining());
//				var dbuf = ByteBuffer.allocateDirect(8 * 1024);
//				var charset = Charset.forName("US-ASCII");
//				var encoder = charset.newEncoder();
//				var decoder = charset.newDecoder();
//				for (var s : new String[] { null, "EHLO " + hostname, "AUTH LOGIN",
//						Base64.getEncoder().encodeToString(username.getBytes()),
//						Base64.getEncoder().encodeToString(password.getBytes()), "MAIL FROM:<" + from + ">",
//						"RCPT TO:<" + to + ">", "DATA",
//						"Date: " + date.format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\nFrom: " + from + "\nTo: " + to
//								+ "\nMessage-ID: <"
//								+ ThreadLocalRandom.current().ints(25, '0', '9' + 1).mapToObj(Character::toString)
//										.collect(Collectors.joining(""))
//								+ "@" + hostname + ">\nSubject: " + subject
//								+ "\nMIME-Version: 1.0\nContent-Type: multipart/alternative; boundary=\"" + b
//								+ "\"\n\n--" + b
//								+ "\nContent-Type: text/plain; charset=us-ascii\nContent-Transfer-Encoding: 7bit\n\n"
//								+ message + "\n--" + b
//								+ "\nContent-Type: text/html; charset=utf-8\nContent-Transfer-Encoding: 7bit\n\n"
//								+ htmlMessage + "\n--" + b + "--\n.",
//						"QUIT" }) {
//					if (s != null) {
//						System.out.println("C: " + s.replace("\n", "\nC: "));
//						sch.write(encoder.encode(CharBuffer.wrap(s.replace("\n", "\r\n") + "\r\n")));
//					}
//					dbuf.clear();
//					sch.read(dbuf);
//					dbuf.flip();
//					var cb = decoder.decode(dbuf);
//					System.out.println("\t" + cb.toString().replace("\n", "\n\t"));
//				}
//			}
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
}
