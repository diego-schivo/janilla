package com.janilla.smtp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.net.Net;

public class SmtpClient implements AutoCloseable {

	public static void main(String[] args) {
		var s = new SmtpServer();
		{
			var k = Path.of(System.getProperty("user.home"))
					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
			var p = "passphrase".toCharArray();
			var x = Net.getSSLContext(k, p);
			s.setSslContext(x);
		}
		new Thread(s::run, "Server").start();

		synchronized (s) {
			while (s.getAddress() == null) {
				try {
					s.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		try (var c = new SmtpClient()) {
			c.setAddress(s.getAddress());
			{
//				var x = SSLContext.getInstance("TLSv1.3");
//				x.init(null, null, null);
				var k = Path.of(System.getProperty("user.home"))
						.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
				var p = "passphrase".toCharArray();
				var x = Net.getSSLContext(k, p);
				c.setSslContext(x);
			}
			try {
				var u = c.query(e -> {
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);

				u = c.query(e -> {
					try (var crq = (CommandSmtpRequest) e.getRequest()) {
						crq.writeLine("EHLO example.lan");
					}
					var rrs = (ReplySmtpResponse) e.getResponse();
					return Stream.generate(rrs::readLine).takeWhile(x -> x != null).collect(Collectors.joining("\n"));
				});
				System.out.println(u);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			s.stop();
		}
	}

	private InetSocketAddress address;

	private SSLContext sslContext;

	protected SmtpConnection connection;

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public <T> T query(Function<SmtpExchange, T> handler) {
		if (connection == null) {
			try {
				var sc = SocketChannel.open();
				sc.configureBlocking(true);
				sc.connect(address);
				var e = sslContext.createSSLEngine(address.getHostName(), address.getPort());
				e.setUseClientMode(true);
				connection = new SmtpConnection.Builder().channel(sc).sslEngine(e).build();
				connection.setUseClientMode(true);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		try (var rq = connection.newRequest(); var rs = connection.newResponse()) {
			var e = buildExchange(rq, rs);
			return handler.apply(e);
		}
	}

	@Override
	public void close() {
		if (connection != null)
			connection.close();
	}

	protected SmtpExchange buildExchange(SmtpRequest request, SmtpResponse response) {
		var e = new SmtpExchange();
		e.setRequest(request);
		e.setResponse(response);
		return e;
	}
}
