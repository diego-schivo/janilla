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
package com.janilla.websitetemplate.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.blanktemplate.test.BlankTest;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.websitetemplate.fullstack.WebsiteFullstack;

public class WebsiteTest extends BlankTest {

	public static final String[] DI_PACKAGES = Stream
			.concat(Arrays.stream(BlankTest.DI_PACKAGES), Stream.of("com.janilla.websitetemplate.test"))
			.toArray(String[]::new);

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x, false)).toList());
		serve(f, WebsiteTest.class, args.length > 0 ? args[0] : null);
	}

	public WebsiteTest(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "website-template");
	}

	public WebsiteTest(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	@Override
	protected String[] diFullstackPackages() {
		return WebsiteFullstack.DI_PACKAGES;
	}

	@Override
	protected Map<String, List<Path>> resourcePaths() {
		return Map.of("",
				Stream.of("com.janilla.frontend", "com.janilla.blanktemplate.test", "com.janilla.websitetemplate.test")
						.flatMap(x -> Java.getPackagePaths(x, false).filter(Files::isRegularFile)).toList());
	}
}
