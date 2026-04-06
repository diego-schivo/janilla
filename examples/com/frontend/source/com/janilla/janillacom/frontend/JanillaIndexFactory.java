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
package com.janilla.janillacom.frontend;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.frontend.Template;
import com.janilla.frontend.cms.CmsDataFetching;
import com.janilla.web.ResourceMap;
import com.janilla.websitetemplate.frontend.WebsiteIndexFactory;

public class JanillaIndexFactory extends WebsiteIndexFactory {

	public JanillaIndexFactory(ResourceMap resourceMap, CmsDataFetching dataFetching, Properties configuration,
			String configurationKey) {
		super(resourceMap, dataFetching, configuration, configurationKey);
	}

	@Override
	public Template websiteTemplate(String name) {
		return template("website/" + name);
	}

	public Template janillaTemplate(String name) {
		return template(name);
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);
		Stream.of("header", "link", "post").map(this::janillaImportKey).forEach(x -> map.put(x, "/" + x + ".js"));
	}

	@Override
	protected String websiteImportKey(String name) {
		return "website/" + name;
	}

	protected String janillaImportKey(String name) {
		return name;
	}
}
