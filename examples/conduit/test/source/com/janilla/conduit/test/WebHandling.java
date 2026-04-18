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
package com.janilla.conduit.test;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import com.janilla.conduit.fullstack.ConduitFullstack;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;

class WebHandling {

	protected static final AtomicBoolean TEST_ONGOING = new AtomicBoolean();

	protected final IndexFactory indexFactory;

	protected final ConduitFullstack fullstack;

	public WebHandling(IndexFactory indexFactory, ConduitFullstack fullstack) {
		this.indexFactory = indexFactory;
		this.fullstack = fullstack;
	}

	@Handle(method = "GET", path = "/")
	public Index home(HttpExchange exchange) {
		return indexFactory.newIndex(exchange);
	}

	@Handle(method = "POST", path = "/test/start")
	public void start() throws IOException {
//		IO.println("Test.start, this=" + this);
		if (TEST_ONGOING.getAndSet(true))
			throw new IllegalStateException();

		var d = fullstack.backend().persistence().database();
		var ch1 = (FileChannel) d.channel().channel();
		try (var ch2 = Channels.newChannel(getClass().getResourceAsStream("conduit-test.db"))) {
			var s = ch1.transferFrom(ch2, 0, Long.MAX_VALUE);
			ch1.truncate(s);
		}
		d.pageCache().clear();
	}

	@Handle(method = "POST", path = "/test/stop")
	public void stop() {
//		IO.println("Test.stop, this=" + this);
		if (!TEST_ONGOING.getAndSet(false))
			throw new IllegalStateException();
	}
}
