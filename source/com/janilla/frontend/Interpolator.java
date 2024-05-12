/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
package com.janilla.frontend;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.html.Html;
import com.janilla.javascript.JavaScript;

public record Interpolator(Token[] tokens) implements Function<Function<Object, Object>, String> {

	public static Interpolator of(String template, Language language) {
		var i = 0;
		var b = Stream.<Token>builder();
		for (var rr = language.expression.matcher(template).results().iterator(); rr.hasNext();) {
			var r = rr.next();
			var j = r.start();
			if (j > i) {
				var i0 = i;
				var j0 = j;
				b.add(e -> template.substring(i0, j0));
			}
			b.add(language.toToken.apply(r));
			i = r.end();
		}
		var j = template.length();
		if (j > i) {
			var i0 = i;
			var j0 = j;
			b.add(e -> template.substring(i0, j0));
		}
		return new Interpolator(b.build().toArray(Token[]::new));
	}

	@Override
	public String apply(Function<Object, Object> evaluator) {
		var b = Stream.<String>builder();
		for (var t : tokens) {
			var s = t.apply(evaluator);
			if (s != null && !s.isEmpty())
				b.add(s);
		}
		return b.build().collect(Collectors.joining(""));
	}

	public interface Token extends Function<Function<Object, Object>, String> {
	}

	public enum Language {

		HTML(TemplatesWeb.expression, r -> {
			var g1 = r.group(1);
			var g2 = r.group(2);
			var g3 = r.group(3);
			return e -> {
				var f = Objects.toString(g2, g3);
				var s = Objects.toString(e.apply(f), null);
				if (g1 == null && g2 != null)
					s = Html.escape(s);
				return s;
			};
		}), JAVASCRIPT(Pattern.compile("\\$\\{([\\w.-]*?)}|/\\*\\$\\{([\\w.-]*?)}\\*/"), r -> {
			var g1 = r.group(1);
			var g2 = r.group(2);
			return e -> {
				var f = Objects.toString(g1, g2);
				var s = Objects.toString(e.apply(f), null);
				if (g1 != null)
					s = JavaScript.escape(s);
				return s;
			};
		});

		Pattern expression;

		Function<MatchResult, Token> toToken;

		Language(Pattern expression, Function<MatchResult, Token> toToken) {
			this.expression = expression;
			this.toToken = toToken;
		}
	}
}
