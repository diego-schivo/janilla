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
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.janilla.io.FilterByteChannel;
import com.janilla.io.FilterChannel;

public class Server {

	private SocketAddress address;

	private Protocol protocol;

	private ServerSocketChannel channel;

	private final Map<Connection, Long> lastUsed = new HashMap<>();

	private final Lock lastUsedLock = new ReentrantLock();

	// *******************
	// Getters and Setters

	public SocketAddress getAddress() {
		return address;
	}

	public void setAddress(SocketAddress address) {
		this.address = address;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	// Getters and Setters
	// *******************

	public void serve() {
		try {
			channel = ServerSocketChannel.open();
//			System.out.println("Server.serve, channel=" + channel + ", channel.isBlocking()=" + channel.isBlocking());
			channel.socket().bind(address);
			Thread.startVirtualThread(() -> {
				for (;;) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					shutdownConnections();
				}
			});
//			System.out.println("Server.serve, address=" + address);
			for (;;) {
				accept();
//				if (bar != null)
//					throw bar;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//	private Map<String, Integer> foo = new ConcurrentHashMap<>();
//
//	private RuntimeException bar;

	protected void accept() throws IOException {
		var sc = channel.accept();
//		System.out.println(LocalTime.now() + ", Server.accept, sc=" + sc + ", sc.isBlocking()=" + sc.isBlocking());
		Thread.startVirtualThread(() -> {
			class A {

				Connection c;
			}
			var a = new A();
			a.c = protocol.buildConnection(new FilterByteChannel(sc) {

				@Override
				public int read(ByteBuffer dst) throws IOException {
					if (!updateLastUsed(a.c, false))
						throw new IOException();
					return super.read(dst);
				}

				@Override
				public int write(ByteBuffer src) throws IOException {
					if (!updateLastUsed(a.c, false))
						throw new IOException();
					return super.write(src);
				}
			});
			for (var f = true;; f = false) {
				if (!updateLastUsed(a.c, f))
					break;

//				var k = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ISO_DATE_TIME);
//				var v = (int) foo.compute(k, (x, y) -> y == null ? 1 : y + 1);
//				if (v > 10000) {
//					bar = new RuntimeException(k + " " + v);
//					return;
//				}
//				System.out.println("Server.accept, c=" + c.getId() + ", handle " + k + " " + v);

				try {
					if (!protocol.handle(a.c))
						break;
				} catch (Exception e) {
//					System.out.println("accept, c=" + c.getId() + ", e=" + e);
					e.printStackTrace();
					break;
				}
			}
//			System.out.println("Server.accept, c=" + c.getId() + ", exit");
		});
	}

	boolean updateLastUsed(Connection connection, boolean insert) {
		lastUsedLock.lock();
		try {
			var x = insert || lastUsed.containsKey(connection);
			if (x)
				lastUsed.put(connection, System.currentTimeMillis());
			return x;
		} finally {
			lastUsedLock.unlock();
		}
	}

	protected void shutdownConnections() {
		Set<Connection> cc = new HashSet<>();
		lastUsedLock.lock();
		try {
			var t1 = System.currentTimeMillis();
			for (var ee = lastUsed.entrySet().iterator(); ee.hasNext();) {
				var e = ee.next();
				var t2 = e.getValue() + 10 * 1000;
				if (t1 >= t2) {
					ee.remove();
					cc.add(e.getKey());
				}
			}
		} finally {
			lastUsedLock.unlock();
		}
		for (var c : cc) {
//			System.out.println("shutdown, c=" + c.getId());

//			String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^\\d.]", "");
//			try {
//				Runtime.getRuntime().exec("jcmd " + pid + " Thread.dump_to_file -format=json dump.json");
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}

			Channel ch = c.getChannel();
			while (ch instanceof FilterChannel fch)
				ch = fch.channel();
			var sch = (SocketChannel) ch;
			try {
				sch.shutdownInput();
				sch.shutdownOutput();
				sch.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
