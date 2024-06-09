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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

public class StubHandler implements SmtpServer.Handler {

	@Override
	public SmtpConnection buildConnection(SocketChannel channel, SSLEngine engine) {
		return new SmtpConnection.Builder().connection(new Bar()).channel(channel).sslEngine(engine).build();
	}

	@Override
	public boolean handle(SmtpExchange exchange) {
		var rq = exchange.getRequest();
		var rrs = (ReplySmtpResponse) exchange.getResponse();
		var c = (StubHandler.Bar) exchange.getConnection();
		InetAddress h;
		try {
			h = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		switch (c.getRequestType()) {
		case EMPTY:
			rrs.writeLine("220 " + h.getCanonicalHostName() + " (" + h.getHostName() + ") Foobar ESMTP Service ready");
			c.setRequestType(SmtpRequest.Type.COMMAND);
			return true;
		case COMMAND:
			var d = ((CommandSmtpRequest) rq).readLine();
			if (d == null)
				return false;
			else if (d.startsWith("EHLO ")) {
				try {
					rrs.writeLine("250 " + h.getCanonicalHostName() + " Hello " + d.substring("EHLO ".length()) + " ["
							+ ((InetSocketAddress) c.socketChannel().getRemoteAddress()).getAddress().getHostAddress()
							+ "]");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				rrs.writeLine("250 8BITMIME");
				rrs.writeLine("250 AUTH LOGIN PLAIN");
				rrs.writeLine("250 SIZE 140000000");
				return true;
			} else if (d.startsWith("MAIL FROM:")) {
				rrs.writeLine("250 Requested mail action okay, completed");
				return true;
			} else if (d.startsWith("RCPT TO:")) {
				rrs.writeLine("250 OK");
				return true;
			} else
				switch (d) {
				case "AUTH LOGIN":
					rrs.writeLine("334 VXNlcm5hbWU6");
					c.state = 1;
					return true;
				case "DATA":
					rrs.writeLine("354 Start mail input; end with <CRLF>.<CRLF>");
					c.setRequestType(SmtpRequest.Type.DATA);
					return true;
				case "QUIT":
					rrs.writeLine("221 " + h.getCanonicalHostName() + " Service closing transmission channel");
					return false;
				default:
					switch (c.state) {
					case 1:
						rrs.writeLine("334 UGFzc3dvcmQ6");
						c.state = 2;
						return true;
					case 2:
						rrs.writeLine("235 Authentication succeeded");
						c.state = 0;
						return true;
					default:
						throw new RuntimeException();
					}
				}
		case DATA:
			var drq = (DataSmtpRequest) rq;
			while (drq.readHeader() != null)
				;
			try {
				new String(((InputStream) drq.getBody()).readAllBytes());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			rrs.writeLine("250 Requested mail action okay, completed: id=012345-67890abcde-fghijkl");
			c.setRequestType(SmtpRequest.Type.COMMAND);
			return true;
		}
		return false;
	}

	static class Bar extends SmtpConnection {

		int state;
	}
}
