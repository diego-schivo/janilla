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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.backend.JanillaBackend;
import com.janilla.janillacom.frontend.JanillaFrontend;
import com.janilla.java.Java;
import com.janilla.websitetemplate.fullstack.WebsiteFullstack;

public class JanillaFullstack extends WebsiteFullstack {

	public static Stream<Class<?>> diTypes() {
		return Stream.concat(WebsiteFullstack.diTypes(), Java.getPackageTypes("com.janilla.janillacom.fullstack"));
	};

	public static void main(String[] args) {
		IO.println("pid=" + ProcessHandle.current().pid());
		var r = Runtime.getRuntime();
		IO.println("maxMemory=" + r.maxMemory());

		Thread.startVirtualThread(() -> {
			for (;;) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
					break;
				}
//				IO.println("freeMemory=" + r.freeMemory() + ", maxMemory=" + r.maxMemory() + ", totalMemory="
//						+ r.totalMemory());
				var fm = r.freeMemory();
				var tm = r.totalMemory();
				var um = tm - fm;
				var p = BigDecimal.valueOf(um * 100).divide(BigDecimal.valueOf(tm), RoundingMode.HALF_UP);
				IO.println(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + " " + um + "/" + tm + " (" + p + "%)");
			}
		});

		var f = new DefaultDiFactory(diTypes().toList(), "fullstack");
		try {
			serve(f, args.length > 0 ? args[0] : null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public JanillaFullstack(DiFactory diFactory, Path configurationFile) {
		super(diFactory, configurationFile, "janilla-com");
	}

	@Override
	protected Stream<Class<?>> diBackendTypes() {
		return Stream.concat(JanillaBackend.diTypes(), Java.getPackageTypes("com.janilla.janillacom.fullstack"));
	}

	@Override
	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.concat(JanillaFrontend.diTypes(), Java.getPackageTypes("com.janilla.janillacom.fullstack"));
	}
}
