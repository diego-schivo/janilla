package com.janilla.smtp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.net.SslByteChannel;

public class SmtpClient {

	private static String host = "smtp.example.com";
	private static int port = 465;
	private static Charset charset = Charset.forName("US-ASCII");
	private static CharsetEncoder encoder = charset.newEncoder();
	private static CharsetDecoder decoder = charset.newDecoder();

	public static void main(String[] args) {
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
				var username = "foo.bar@example.com";
				var password = "**********";
				var d = new Data(OffsetDateTime.now(), "foo.bar@example.com", "baz.qux@example.com",
						"Multipart alternative", """
								Hello Alice.
								This is a test message with 5 header fields and 4 lines in the message body.
								Your friend,
								Bob
								""",
						"""
								<p>
								  Hello Alice.<br>
								  This is a <b style='color:red;'>test message</b> with 5 header fields and 4 lines in the message body.<br>
								  Your friend,<br>
								  Bob
								</p>
								""");
				var b = "--=_Part_" + ThreadLocalRandom.current().ints(24, '0', '9' + 1).mapToObj(Character::toString)
						.collect(Collectors.joining());
				var dbuf = ByteBuffer.allocateDirect(8 * 1024);
				for (var s : new String[] { null, "EHLO " + hostname, "AUTH LOGIN",
						Base64.getEncoder().encodeToString(username.getBytes()),
						Base64.getEncoder().encodeToString(password.getBytes()), "MAIL FROM:<" + d.from + ">",
						"RCPT TO:<" + d.to + ">", "DATA",
						"Date: " + d.date.format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\nFrom: " + d.from + "\nTo: "
								+ d.to + "\nMessage-ID: <"
								+ ThreadLocalRandom.current().ints(25, '0', '9' + 1).mapToObj(Character::toString)
										.collect(Collectors.joining(""))
								+ "@" + hostname + ">\nSubject: " + d.subject
								+ "\nMIME-Version: 1.0\nContent-Type: multipart/alternative; boundary=\"" + b + "\"\n--"
								+ b
								+ "\nContent-Type: text/plain; charset=us-ascii\nContent-Transfer-Encoding: 7bit\n\n"
								+ d.message + "--" + b
								+ "\nContent-Type: text/html; charset=utf-8\nContent-Transfer-Encoding: 7bit\n\n"
								+ d.htmlMessage + "--" + b + "--\n.",
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

	private record Data(OffsetDateTime date, String from, String to, String subject, String message,
			String htmlMessage) {
	}
}
