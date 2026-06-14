/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.janillacom.fullstack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.DefaultHttpServer;
import com.janilla.http.Frame;
import com.janilla.http.FrameTransfer;
import com.janilla.http.HeadersFrame;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.ioc.Scope;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.janillacom.backend.JanillaBackend;
import com.janilla.janillacom.frontend.JanillaFrontend;
import com.janilla.net.AmountLimiter;
import com.janilla.net.Blacklister;
import com.janilla.net.FilterTransfer;
import com.janilla.net.RateLimiter;
import com.janilla.net.Transfer;

@Scope("fullstack")
class HttpServerImpl extends DefaultHttpServer {

	protected static final ScopedValue<AmountLimiter> REQUEST_SIZE_LIMITER = ScopedValue.newInstance();

	private static final Logger LOGGER = System.getLogger(HttpServerImpl.class.getName());

	protected final JanillaBackend backend;

	protected final Blacklister blacklister = new Blacklister(
			Pattern.compile("aws|config|docker|env|info|node|php|sql|wp|yml"));

	protected final JanillaFrontend frontend;

	protected final RateLimiter<InetAddress> requestRateLimiter = new RateLimiter<>(1000, 60);

	public HttpServerImpl(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler, JanillaFrontend frontend,
			JanillaBackend backend) {
		super(endpoint, sslContext, handler);
		this.frontend = frontend;
		this.backend = backend;
	}

	@Override
	protected Thread startThread(SocketChannel channel) {
		var a = address(channel);

		if (blacklister.test(a))
			throw new IllegalStateException("Blacklisted " + a);

		return super.startThread(channel);
	}

	@Override
	protected void handleConnection(Transfer transfer) {
		var l = new AmountLimiter(8192);
		ScopedValue.where(REQUEST_SIZE_LIMITER, l).run(() -> {
			var t = new FilterTransfer(transfer) {

				@Override
				public int read() {
					var n = super.read();

					if (n > 0 && !l.test(n))
						throw new IllegalStateException("Request size limit exceeded");

					return n;
				}
			};

			HttpServerImpl.super.handleConnection(t);
		});
	}

	@Override
	protected void handleEndHeaders1(List<String> lines) {
		var a = address(SOCKET_CHANNEL.get());
		var l = lines.getFirst();
		LOGGER.log(Level.DEBUG, a + " " + l);

		if (blacklister.test(a, l))
			throw new IllegalStateException("Blacklisted " + a);
	}

	@Override
	protected void handleEndHeaders2(List<Frame> frames, FrameTransfer transfer) {
		var a = address(SOCKET_CHANNEL.get());
		var s = frames.stream().flatMap(x -> x instanceof HeadersFrame y ? y.fields().stream() : Stream.empty())
				.filter(x -> x.name().equals(":method") || x.name().equals(":path"))
				.sorted(Comparator.comparing(x -> x.name())).map(x -> x.value()).collect(Collectors.joining(" "));
		LOGGER.log(Level.DEBUG, a + " " + s);

		if (blacklister.test(a, s))
			throw new IllegalStateException("Blacklisted " + a);
	}

	@Override
	protected void handleEndStream(List<Frame> frames, FrameTransfer transfer) {
		var c = SOCKET_CHANNEL.get();
		var l = REQUEST_SIZE_LIMITER.get();
		Thread.startVirtualThread(() -> ScopedValue.where(SOCKET_CHANNEL, c)
				.run(() -> ScopedValue.where(REQUEST_SIZE_LIMITER, l).run(() -> handleStream(frames, transfer))));
	}

	@Override
	public void exchange(HttpRequest request, HttpResponse response) {
		REQUEST_SIZE_LIMITER.get().reset();

		var a = address(SOCKET_CHANNEL.get());
		if (!requestRateLimiter.test(a))
			throw new IllegalStateException("Request rate limit exceeded (address=" + a + ")");

		var wa = request.getPath().contains("/api/") ? backend.backend(request) : frontend.frontend(request);
		LOGGER.log(Level.DEBUG, "app={0}", wa);

		ScopedValue.where(JanillaDomain.WEB_APP, wa).run(() -> super.exchange(request, response));
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var a = JanillaDomain.WEB_APP.get();
		var c = a.diFactory().classFor(HttpExchange.class);
		return c != null ? a.diFactory().newInstance(c, Map.of("request", request, "response", response))
				: super.createExchange(request, response);
	}

	protected InetAddress address(SocketChannel channel) {
		InetAddress a;
		try {
			a = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return a;
	}
}
