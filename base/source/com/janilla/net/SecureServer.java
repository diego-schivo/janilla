/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

public abstract class SecureServer {

	protected static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);

	protected final SSLContext sslContext;

	protected final SocketAddress endpoint;

	protected final Map<SocketChannel, ThreadAndInstant> lastUsed = new ConcurrentHashMap<>();

	public SecureServer(SSLContext sslContext, SocketAddress endpoint) {
		this.sslContext = sslContext;
		this.endpoint = endpoint;
	}

	public void serve() {
//		IO.println("SecureServer.serve");
		Thread.startVirtualThread(this::shutdownConnections);

		try (var s = ServerSocketChannel.open()) {
			s.socket().bind(endpoint);
			for (;;) {
				var ch = s.accept();
//				IO.println("SecureServer.serve, ch=" + ch);
				var th = Thread.startVirtualThread(() -> handleConnection(ch));
				lastUsed.put(ch, new ThreadAndInstant(th, Instant.now()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void handleConnection(SocketChannel channel) {
		try (var ch = channel) {
			handleConnection(new FilterTransfer(new SecureTransfer(ch, createSslEngine()) {

				@Override
				public int read() throws IOException {
					updateLastUsed(ch);
					try {
						return super.read();
					} finally {
						updateLastUsed(ch);
					}
				}

				@Override
				public void write() throws IOException {
					try {
						super.write();
					} finally {
						updateLastUsed(ch);
					}
				}
			}));
		} catch (SSLHandshakeException | ClosedByInterruptException e) {
			IO.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected SSLEngine createSslEngine() {
		var x = sslContext.createSSLEngine();
		x.setUseClientMode(false);
		return x;
	}

	protected abstract void handleConnection(Transfer transfer) throws IOException;

	protected ThreadAndInstant updateLastUsed(SocketChannel channel) {
		return lastUsed.computeIfPresent(channel, (_, x) -> new ThreadAndInstant(x.thread(), Instant.now()));
	}

	protected void shutdownConnections() {
		for (;;) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				break;
			}

			Map<SocketChannel, ThreadAndInstant> m = new HashMap<>();
//			IO.println("SecureServer.shutdownConnections, lastUsed=" + lastUsed.size());
			var i0 = Instant.now().minus(CONNECTION_TIMEOUT);
			for (var it = lastUsed.entrySet().iterator(); it.hasNext();) {
				var kv = it.next();
				var i = kv.getValue().instant();
				if (!i.isAfter(i0)) {
					it.remove();
					m.put(kv.getKey(), kv.getValue());
				}
			}
			for (var kv : m.entrySet()) {
				var ch = kv.getKey();
				var th = kv.getValue().thread();
//				IO.println("SecureServer.shutdownConnections, ch=" + ch);

//			String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^\\d.]", "");
//			try {
//				Runtime.getRuntime().exec("jcmd " + pid + " Thread.dump_to_file -format=json dump.json");
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}

				if (th.isAlive()) {
//				IO.println("SecureServer.shutdownConnections, th=" + th);
					th.interrupt();
				}

				if (ch.isOpen()) {
//				IO.println("SecureServer.shutdownConnections, ch=" + ch);
					try {
						ch.shutdownInput();
						ch.shutdownOutput();
						ch.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
