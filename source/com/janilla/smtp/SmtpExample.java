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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import com.janilla.net.Net;
import com.janilla.net.SslByteChannel;

public class SmtpExample {

	private static int port = 8443;
	private static Charset charset = Charset.forName("US-ASCII");
	private static CharsetEncoder encoder = charset.newEncoder();
	private static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) {
		var s = Thread.startVirtualThread(new Server());

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		var c = Thread.startVirtualThread(new Client());

		try {
			c.join();
			s.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static class Server implements Runnable {

		private String[] ss = { "220 smtp.example.com ESMTP Postfix",
				"250 Hello relay.example.org, I am glad to meet you", "250 Ok", "250 Ok", "250 Ok",
				"354 End data with <CR><LF>.<CR><LF>", "250 Ok: queued as 12345", "221 Bye" };

		@Override
		public void run() {
			try {
				var ssc = ServerSocketChannel.open();
				var isa = new InetSocketAddress(InetAddress.getLocalHost(), port);
				ssc.socket().bind(isa);
				SSLContext ssl;
				try (var is = Net.class.getResourceAsStream("testkeys")) {
					ssl = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
				}
				try (var ch = ssc.accept()) {
					var se = ssl.createSSLEngine();
					se.setUseClientMode(false);
					try (var sch = new SslByteChannel(ch, se)) {
						var dbuf = ByteBuffer.allocateDirect(1024);
						var i = 0;
						for (var s : ss) {
							if (i > 0) {
								dbuf.clear();
								sch.read(dbuf);
								dbuf.flip();
								var cb = decoder.decode(dbuf);
								System.out.println("\t" + cb.toString().replace("\n", "\n\t"));
							}
							if (s != null) {
								System.out.println("S: " + s.replace("\n", "\nS: "));
								sch.write(encoder.encode(CharBuffer.wrap(s.replace("\n", "\r\n") + "\r\n")));
							}
							i++;
						}
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static class Client implements Runnable {

		private String[] ss = { null, "HELO relay.example.org", "MAIL FROM:<bob@example.org>",
				"RCPT TO:<alice@example.com>", "RCPT TO:<theboss@example.com>", "DATA", """
						From: "Bob Example" <bob@example.org>
						To: "Alice Example" <alice@example.com>
						Cc: theboss@example.com
						Date: Tue, 15 Jan 2008 16:02:43 -0500
						Subject: Test message

						Hello Alice.
						This is a test message with 5 header fields and 4 lines in the message body.
						Your friend,
						Bob
						.""", "QUIT" };

		@Override
		public void run() {
			SSLContext ssl;
//			try (var is = Net.class.getResourceAsStream("testkeys")) {
//				ssl = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
			try {
				ssl = SSLContext.getInstance("TLSv1.3");
				ssl.init(null, null, null);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			try (var ch = SocketChannel.open()) {
				var isa = new InetSocketAddress(InetAddress.getLocalHost(), port);
				ch.connect(isa);
				var se = ssl.createSSLEngine();
				se.setUseClientMode(true);
				try (var sch = new SslByteChannel(ch, se)) {
					var dbuf = ByteBuffer.allocateDirect(1024);
					for (var s : ss) {
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
	}
}
