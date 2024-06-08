package com.janilla.smtp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

public class SmtpServer implements Runnable {

	private volatile int port;

	private volatile SSLContext sslContext;

	protected volatile InetSocketAddress address;

	protected volatile Selector selector;

	protected volatile boolean stop;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	@Override
	public void run() {
		try {
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(true);
			ssc.socket().bind(new InetSocketAddress(port));
			address = (InetSocketAddress) ssc.getLocalAddress();

			ssc.configureBlocking(false);
			selector = SelectorProvider.provider().openSelector();
			ssc.register(selector, SelectionKey.OP_ACCEPT);

			synchronized (this) {
				notifyAll();
			}

			for (;;) {
				selector.select(1000);

				if (stop)
					break;

				var ccb = Stream.<SmtpConnection>builder();
				var n = 0;
				for (var i = selector.selectedKeys().iterator(); i.hasNext();) {
					var k = i.next();

//					System.out.println("k=" + k);

					i.remove();

					if (!k.isValid())
						continue;

					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
						var e = sslContext.createSSLEngine();
						e.setUseClientMode(false);
						var c = new SmtpConnection.Builder().channel(sc).sslEngine(e).build();
						sc.register(selector, SelectionKey.OP_WRITE).attach(c);
					}

					if (k.isReadable() || k.isWritable()) {
						var c = (SmtpConnection) k.attachment();
						k.cancel();
						ccb.add(c);
						n++;
					}
				}

				if (n == 0)
					continue;

//				System.out.println("SmtpServer.serve n=" + n + " " + LocalTime.now());
//				var m = System.currentTimeMillis();

				selector.selectNow();

				for (var cc = ccb.build().iterator(); cc.hasNext();) {
					var c = cc.next();
					handle(c);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void stop() {
		stop = true;
		selector.wakeup();
	}

	protected void handle(SmtpConnection connection) {
		try {
			var k = true;
			try (var rq = connection.newRequest(); var rs = connection.newResponse()) {
				@SuppressWarnings("resource")
				var rrs = (ReplySmtpResponse) rs;
				switch (connection.getState()) {
				case NEW:
					rrs.writeLine("220 example.com (abcde012) Foobar ESMTP Service ready");
					connection.setState(SmtpConnection.State.COMMAND);
					break;
				case COMMAND:
					var c = ((CommandSmtpRequest) rq).readLine();
					if (c != null)
						switch (c) {
						case "EHLO example.lan":
							rrs.writeLine("250 example.com Hello example.lan [123.45.67.890]");
							rrs.writeLine("250 8BITMIME");
							rrs.writeLine("250 AUTH LOGIN PLAIN");
							rrs.writeLine("250 SIZE 140000000");
							break;
						case "AUTH LOGIN":
							rrs.writeLine("334 VXNlcm5hbWU6");
							break;
						case "********************************":
							rrs.writeLine("334 UGFzc3dvcmQ6");
							break;
						case "****************":
							rrs.writeLine("235 Authentication succeeded");
							break;
						case "MAIL FROM:<foo@example.com>":
							rrs.writeLine("250 Requested mail action okay, completed");
							break;
						case "RCPT TO:<bar@example.com>":
							rrs.writeLine("250 OK");
							break;
						case "DATA":
							rrs.writeLine("354 Start mail input; end with <CRLF>.<CRLF>");
							connection.setState(SmtpConnection.State.DATA);
							break;
						case "QUIT":
							rrs.writeLine("221 example.com Service closing transmission channel");
							k = false;
							break;
						default:
							throw new RuntimeException();
						}
					break;
				case DATA:
					var drq = (DataSmtpRequest) rq;
					@SuppressWarnings("unused") String h;
					while ((h = drq.readHeader()) != null)
						;
					@SuppressWarnings("unused")
					var b = new String(drq.getBody().readAllBytes());
					rrs.writeLine("250 Requested mail action okay, completed: id=012345-67890abcde-fghijkl");
					connection.setState(SmtpConnection.State.COMMAND);
					break;
				}
			} catch (Exception e) {
				printStackTrace(e);
				k = false;
			}

			if (k) {
				connection.channel().register(selector, SelectionKey.OP_READ).attach(connection);
				selector.wakeup();
			} else
				connection.close();
		} catch (Exception e) {
			printStackTrace(e);
		}
	}

	static void printStackTrace(Exception exception) {
		exception.printStackTrace();
	}
}
