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

import java.lang.System.Logger;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class SimpleLogger implements Logger {

	protected final String name;

	protected final Level level;

	public SimpleLogger(String name, Level level) {
		this.name = name;
		this.level = level;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isLoggable(Level level) {
		return level != Level.OFF && level.ordinal() >= this.level.ordinal();
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if (isLoggable(level))
			IO.println(foo() + " " + msg);
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if (isLoggable(level))
			IO.println(foo() + " " + MessageFormat.format(format, params));
	}

	protected String foo() {
		class A {
			static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		}
		var f = A.WALKER.walk(x -> x.filter(y -> !y.getDeclaringClass().equals(SimpleLogger.class)
				&& !y.getDeclaringClass().equals(System.Logger.class)).findFirst().get());
		return f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "." + f.getMethodName();
	}
}
