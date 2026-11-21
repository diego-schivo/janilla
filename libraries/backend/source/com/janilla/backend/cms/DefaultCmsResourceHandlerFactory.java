/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.backend.cms;

import java.nio.file.Path;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.web.AbstractHttpHandlerFactory;
import com.janilla.web.WebAppConfig;

public class DefaultCmsResourceHandlerFactory extends AbstractHttpHandlerFactory implements CmsResourceHandlerFactory {

	protected final CmsResourceHandling handling;

	public DefaultCmsResourceHandlerFactory(WebAppConfig config, HttpHandlerFactory rootFactory, DiFactory diFactory,
			CmsResourceHandling cmsResourceHandling) {
		super(config, rootFactory, diFactory);

		this.handling = cmsResourceHandling;
	}

	@Override
	public HttpHandler newHandler(Object input) {
		var p = input instanceof HttpRequest x ? webAppPath(x) : null;
		var n = p != null && p.startsWith("/api/media/") ? p.substring("/api/media/".length()) : null;
		var f = n != null && n.indexOf('/') == -1 ? Path.of(n) : null;

		return f != null && handling.canHandle(f) ? x -> handling.handle(f, x.response()) : null;
	}
}
