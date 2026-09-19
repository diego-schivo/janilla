/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
package com.janilla.blanktemplate.test;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.janilla.blanktemplate.fullstack.BlankFullstack;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Copier;
import com.janilla.java.Java;
import com.janilla.web.Handle;

public class WebHandling {

	protected static final AtomicBoolean TEST_ONGOING = new AtomicBoolean();

	protected final Copier copier;

	protected final DiFactory diFactory;

	protected final BlankFullstack<?, ?> fullstack;

	protected final IndexFactory indexFactory;

	public WebHandling(IndexFactory indexFactory, BlankFullstack<?, ?> fullstack, DiFactory diFactory, Copier copier) {
		this.indexFactory = indexFactory;
		this.fullstack = fullstack;
		this.diFactory = diFactory;
		this.copier = copier;
	}

	@Handle(method = "GET", path = "/")
	public Index home() {
		return indexFactory.newIndex();
	}

	@Handle(method = "POST", path = "/test/start")
	public void start(HttpExchange exchange) throws IOException {
//		IO.println("Test.start, this=" + this);
		if (TEST_ONGOING.getAndSet(true))
			throw new IllegalStateException();

		var d = fullstack.backend().persistence().database();
		var f = fullstack.backend().config().database().file();
		var ch1 = (FileChannel) d.channel().channel();
		try (var ch2 = Channels
				.newChannel(diFactory.context().getClass().getResourceAsStream(f.substring(f.lastIndexOf('/') + 1)))) {
			var s = ch1.transferFrom(ch2, 0, Long.MAX_VALUE);
			ch1.truncate(s);
		}
		d.pageCache().clear();

		var m = Java.hashMap("value", null, "path", "/", "httpOnly", true, "sameSite", "Lax", "expires",
				ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));
		exchange.request().getHeaderValues("cookie").flatMap(x -> Arrays.stream(x.split("; "))).map(HttpCookie::parse)
				.forEach(x -> exchange.response().addHeaderValue("set-cookie", (copier.copy(m, x)).format()));
	}

	@Handle(method = "POST", path = "/test/stop")
	public void stop() {
//		IO.println("Test.stop, this=" + this);
		if (!TEST_ONGOING.getAndSet(false))
			throw new IllegalStateException();
	}
}
