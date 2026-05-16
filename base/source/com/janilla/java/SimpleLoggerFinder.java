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
package com.janilla.java;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.System.LoggerFinder;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.json.Json;

public class SimpleLoggerFinder extends LoggerFinder {

	private static final Map<String, Level> TEMP_LEVELS = new HashMap<>(0);

	protected final Map<String, Logger> loggers = new ConcurrentHashMap<>();

	protected volatile Map<String, Level> levels;

	@Override
	public Logger getLogger(String name, Module module) {
//		IO.println("name=" + name);

		if (levels == null)
			initLevels();

		var ll = levels;
		return loggers.computeIfAbsent(name, _ -> ll == null || ll == TEMP_LEVELS ? new DelegatingLogger()
				: new SimpleLogger(name, Optional.ofNullable(ll.get(name)).orElse(Level.INFO)));
	}

	private synchronized void initLevels() {
		if (levels == null) {
			Class<?> c;
			{
				var f = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
						.walk(x -> x.reduce((_, y) -> y).get());
				c = f.getMethodName().equals("main") ? f.getDeclaringClass() : null;
			}
//			IO.println("c=" + c);

			if (c != null) {
				levels = TEMP_LEVELS;

				String s;
				try (var is = c.getResourceAsStream("log.json")) {
					s = is != null ? new String(is.readAllBytes()) : null;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				Map<String, Level> m;
				if (s != null) {
					Field f;
					try {
						f = SimpleLoggerFinder.class.getDeclaredField("levels");
					} catch (NoSuchFieldException e) {
						throw new RuntimeException(e);
					}
					m = new DefaultConverter().convert(Json.parse(s), f.getGenericType());
				} else
					m = Map.of();
//				IO.println("m=" + m);

				for (var e : loggers.entrySet()) {
					// IO.println("e=" + e);

					var n = e.getKey();
					var l = (DelegatingLogger) e.getValue();
					l.delegate = new SimpleLogger(n, Optional.ofNullable(m.get(n)).orElse(Level.INFO));
				}

				levels = m;
			}
		}
	}

	class DelegatingLogger implements Logger {

		volatile Logger delegate;

		@Override
		public String getName() {
			var d = delegate();
			return d != null ? d.getName() : null;
		}

		@Override
		public boolean isLoggable(Level level) {
			var d = delegate();
			return d != null ? d.isLoggable(level) : false;
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
			var d = delegate();
			if (d != null)
				d.log(level, bundle, msg, thrown);
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String format, Object... params) {
			var d = delegate();
			if (d != null)
				d.log(level, bundle, format, params);
		}

		Logger delegate() {
			var d = delegate;
			if (d == null && levels == null) {
				initLevels();
				d = delegate;
			}
			return d;
		}
	}
}
