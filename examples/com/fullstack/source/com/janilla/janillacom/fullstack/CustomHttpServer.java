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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.janilla.ioc.Context;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.janillacom.backend.JanillaBackend;
import com.janilla.janillacom.frontend.JanillaFrontend;
import com.janilla.java.JavaReflect;

@Context("fullstack")
public class CustomHttpServer extends DefaultHttpServer {

	protected static final Pattern BOT_REGEX = Pattern.compile("aws|config|docker|env|info|node|php|sql|wp|yml");

	protected final JanillaBackend backend;

	protected final JanillaFrontend frontend;

	protected final Set<String> blacklist = new HashSet<>();

	public CustomHttpServer(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler, JanillaBackend backend,
			JanillaFrontend frontend) {
		super(endpoint, sslContext, handler);
		this.backend = backend;
		this.frontend = frontend;
	}

	@Override
	protected Thread startThread(SocketChannel channel) {
		String a;
		try {
			a = ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (blacklist.contains(a))
			throw new RuntimeException("Blacklisted " + a);

		return super.startThread(channel);
	}

	@Override
	protected void handleEndHeaders1(List<String> lines) {
		var dt = LocalDateTime.now();

		SocketAddress a;
		try {
			a = SOCKET_CHANNEL.get().getRemoteAddress();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var l = lines.getFirst();

		IO.println(dt.truncatedTo(ChronoUnit.SECONDS) + " " + a + " " + l);

		if (BOT_REGEX.matcher(l).find()) {
			blacklist.add(((InetSocketAddress) a).getAddress().getHostAddress());
			throw new RuntimeException("Blacklisted " + ((InetSocketAddress) a).getAddress().getHostAddress());
		}
	}

	@Override
	protected void handleEndHeaders2(List<Frame> frames, FrameTransfer transfer) {
		var dt = LocalDateTime.now();

		SocketAddress a;
		try {
			a = SOCKET_CHANNEL.get().getRemoteAddress();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var s = frames.stream().flatMap(x -> x instanceof HeadersFrame y ? y.fields().stream() : Stream.empty())
				.filter(x -> x.name().equals(":method") || x.name().equals(":path"))
				.sorted(Comparator.comparing(x -> x.name())).map(x -> x.value()).collect(Collectors.joining(" "));

		IO.println(dt.truncatedTo(ChronoUnit.SECONDS) + " " + a + " " + s);

		if (BOT_REGEX.matcher(s).find()) {
			blacklist.add(((InetSocketAddress) a).getAddress().getHostAddress());
			throw new RuntimeException("Blacklisted " + ((InetSocketAddress) a).getAddress().getHostAddress());
		}
	}

	@Override
	protected void exchange(HttpRequest request, HttpResponse response) {
		var a1 = request.getAuthority();
		var a2 = request.getPath().startsWith("/api/") ? backend.application(a1) : frontend.application(a1);
//		IO.println("CustomHttpServer.exchange, a1=" + a1 + ", a2=" + a2);
		ScopedValue.where(JanillaDomain.APPLICATION, a2).run(() -> super.exchange(request, response));
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var a = JanillaDomain.APPLICATION.get();
		var f = (DiFactory) JavaReflect.property(a.getClass(), "diFactory").get(a);
		var c = f.classFor(HttpExchange.class);
		return c != null ? f.newInstance(c, Map.of("request", request, "response", response))
				: super.createExchange(request, response);
	}
}
