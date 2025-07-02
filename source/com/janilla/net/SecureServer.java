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
package com.janilla.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public abstract class SecureServer {

	protected static final int TIMEOUT_MILLIS = 10 * 1000;

	protected final SSLContext sslContext;

	protected final Map<SocketChannel, ThreadAndMillis> lastUsed = new ConcurrentHashMap<>();

	public SecureServer(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void serve(SocketAddress endpoint) {
		Thread.startVirtualThread(() -> {
			for (;;) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
				shutdownConnections();
			}
		});
		try (var sch = ServerSocketChannel.open()) {
			sch.socket().bind(endpoint);
			for (;;) {
				var ch = sch.accept();
//				System.out.println("ch=" + ch + ", ch.isBlocking()=" + ch.isBlocking());
				lastUsed.put(ch, new ThreadAndMillis(Thread.startVirtualThread(() -> {
					try (var _ = ch) {
						handleConnection(new CustomTransfer(ch, createSslEngine()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}), System.currentTimeMillis()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void shutdownConnections() {
		Map<SocketChannel, ThreadAndMillis> m = new HashMap<>();
//			System.out.println("SecureServer.shutdownConnections, lastUsed=" + lastUsed.size());
		var ms = System.currentTimeMillis();
		for (var it = lastUsed.entrySet().iterator(); it.hasNext();) {
			var kv = it.next();
			if (ms >= kv.getValue().millis + TIMEOUT_MILLIS) {
				it.remove();
				m.put(kv.getKey(), kv.getValue());
			}
		}
		for (var kv : m.entrySet()) {
			var ch = kv.getKey();
			var th = kv.getValue().thread;
//			System.out.println("SecureServer.shutdownConnections, sc=" + sc);

//			String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^\\d.]", "");
//			try {
//				Runtime.getRuntime().exec("jcmd " + pid + " Thread.dump_to_file -format=json dump.json");
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}

			if (th.isAlive())
				th.interrupt();

			if (ch.isOpen())
				try {
					ch.shutdownInput();
					ch.shutdownOutput();
					ch.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	protected SSLEngine createSslEngine() {
		var x = sslContext.createSSLEngine();
		x.setUseClientMode(false);
		return x;
	}

	protected abstract void handleConnection(SecureTransfer transfer) throws IOException;

	protected class CustomTransfer extends SecureTransfer {

		public CustomTransfer(ByteChannel channel, SSLEngine engine) {
			super(channel, engine);
		}

		@Override
		public int read() throws IOException {
			updateLastUsed();
			var n = super.read();
			updateLastUsed();
			return n;
		}

		@Override
		public void write() throws IOException {
			updateLastUsed();
			super.write();
			updateLastUsed();
		}

		protected void updateLastUsed() throws IOException {
			if (lastUsed.computeIfPresent((SocketChannel) channel,
					(_, v) -> new ThreadAndMillis(v.thread, System.currentTimeMillis())) == null)
				throw new IOException();
		}
	}

	protected record ThreadAndMillis(Thread thread, long millis) {
	}
}
