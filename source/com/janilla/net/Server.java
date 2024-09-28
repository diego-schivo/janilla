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
package com.janilla.net;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {

	private SocketAddress address;

	private Protocol protocol;

	private ServerSocketChannel channel;

	private Map<Connection, Long> connectionIdleTimes = new HashMap<>();

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

	public ServerSocketChannel getChannel() {
		return channel;
	}

	public void setChannel(ServerSocketChannel channel) {
		this.channel = channel;
	}

	public Map<Connection, Long> getConnectionIdleTimes() {
		return connectionIdleTimes;
	}

	public void setConnectionIdleTimes(Map<Connection, Long> connectionIdleTimes) {
		this.connectionIdleTimes = connectionIdleTimes;
	}

	// Getters and Setters
	// *******************

	public void serve() {
		try {
			channel = ServerSocketChannel.open();
			channel.socket().bind(address);
			Thread.startVirtualThread(() -> {
				for (;;) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					shutdown();
				}
			});
			for (;;)
				accept();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void accept() throws IOException {
		var sc = channel.accept();
		Thread.startVirtualThread(() -> {
			var c = protocol.buildConnection(sc);
//			System.out.println("c=" + c.getId() + ": start");
			for (var f = true;; f = false) {
//				System.out.println("accept, c=" + c.getId());
				synchronized (connectionIdleTimes) {
					if (f || connectionIdleTimes.containsKey(c))
						connectionIdleTimes.put(c, System.currentTimeMillis());
					else
						break;
				}
				try {
					protocol.handle(c);
				} catch (Exception e) {
//					System.out.println("accept, c=" + c.getId() + ", e=" + e);
					e.printStackTrace();
					break;
				}
			}
//			System.out.println("c=" + c.getId() + ": terminate");
		});
	}

	protected void shutdown() {
		Set<Connection> cc = new HashSet<>();
		synchronized (connectionIdleTimes) {
			var t1 = System.currentTimeMillis();
			for (var ee = connectionIdleTimes.entrySet().iterator(); ee.hasNext();) {
				var e = ee.next();
				var t2 = e.getValue() + 10 * 1000;
				if (t1 >= t2) {
					ee.remove();
					cc.add(e.getKey());
				}
			}
		}
		for (var c : cc) {
//			System.out.println("shutdown, c=" + c.getId());
			var sc = c.getChannel();
			try {
				sc.shutdownInput();
				sc.shutdownOutput();
				sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
