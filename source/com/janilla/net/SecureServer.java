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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public abstract class SecureServer {

	protected final SSLContext sslContext;

	protected final Map<SocketChannel, Long> lastUsed = new HashMap<>();

	protected final Lock lastUsedLock = new ReentrantLock();

	public SecureServer(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void serve(SocketAddress endpoint) {
		Thread.startVirtualThread(() -> {
			for (;;) {
				shutdownConnections();
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
		try (var ssc = ServerSocketChannel.open()) {
			ssc.socket().bind(endpoint);
			for (;;) {
				var sc = ssc.accept();
//				System.out.println("sc=" + sc + ", sc.isBlocking()=" + sc.isBlocking());
				lastUsedLock.lock();
				try {
					lastUsed.put(sc, System.currentTimeMillis());
				} finally {
					lastUsedLock.unlock();
				}
				Thread.startVirtualThread(() -> {
					try (var _ = sc) {
						var se = createSslEngine();
						var st = new SecureTransfer(sc, se) {

							@Override
							public void read() throws IOException {
								updateLastUsed();
								super.read();
								updateLastUsed();
							}

							@Override
							public void write() throws IOException {
								updateLastUsed();
								super.write();
								updateLastUsed();
							}

							private void updateLastUsed() throws IOException {
								lastUsedLock.lock();
								try {
									if (lastUsed.containsKey(sc))
										lastUsed.put(sc, System.currentTimeMillis());
									else
										throw new IOException();
								} finally {
									lastUsedLock.unlock();
								}
							}
						};
						handleConnection(st);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void shutdownConnections() {
		Set<SocketChannel> s = new HashSet<>();
		lastUsedLock.lock();
		try {
//			System.out.println("SecureServer.shutdownConnections, lastUsed=" + lastUsed.size());
			var ctm = System.currentTimeMillis();
			for (var esi = lastUsed.entrySet().iterator(); esi.hasNext();) {
				var kv = esi.next();
				if (ctm >= kv.getValue() + 10 * 1000) {
					esi.remove();
					s.add(kv.getKey());
				}
			}
		} finally {
			lastUsedLock.unlock();
		}
		for (var sc : s) {
//			System.out.println("SecureServer.shutdownConnections, sc=" + sc);

//			String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^\\d.]", "");
//			try {
//				Runtime.getRuntime().exec("jcmd " + pid + " Thread.dump_to_file -format=json dump.json");
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}

			if (sc.isOpen())
				try {
					sc.shutdownInput();
					sc.shutdownOutput();
					sc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	protected SSLEngine createSslEngine() {
		var se = sslContext.createSSLEngine();
		se.setUseClientMode(false);
		return se;
	}

	protected abstract void handleConnection(SecureTransfer st) throws IOException;
}
