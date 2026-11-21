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
package com.janilla.web;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class HtmlRenderer<T> extends Renderer<T> {

	protected static final Pattern ATTRIBUTE_COMMENT_TEXT = Pattern.compile(
			String.join("|", "[\\w-]+=\"([^\"]*?\\$\\{.*?\\}.*?)\"", "<!--(\\$\\{.*?\\})-->", "(\\$\\{.*?\\})"));

	protected static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	protected final HtmlEvaluator evaluator;

	public HtmlRenderer(HtmlEvaluator htmlEvaluator) {
		evaluator = htmlEvaluator;
	}

	@Override
	public String apply(T value) {
		var in1 = template(value);
		return ATTRIBUTE_COMMENT_TEXT.matcher(in1).replaceAll(mr1 -> {
			var i = IntStream.rangeClosed(1, mr1.groupCount()).filter(x -> mr1.group(x) != null).findFirst().getAsInt();
			var in2 = mr1.group(i);
//			IO.println(x.group() + " " + i);
			var av = new AnnotatedValue(null, value);
			var oo = new ArrayList<>();
			var s = PLACEHOLDER.matcher(in2).replaceAll(mr2 -> {
				var x = evaluator.evaluate(av, mr2.group(1), oo::add, this);
				if (i != 2)
					x = x.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#x27;")
							.replace("\"", "&quot;");
				return Matcher.quoteReplacement(x);
			});
//			IO.println(s2);
			if (i == 1) {
				var g = mr1.group();
				var g1 = mr1.group(1);
				if (oo.size() == 1 && oo.getFirst() instanceof Boolean b) {
					if (!b)
						return "";
					if (g1.startsWith("${") && g1.indexOf("}") == g1.length() - 1)
						s = "";
				}
				var o = mr1.start(1) - mr1.start();
				s = g.substring(0, o) + s + g.substring(o + g1.length());
			}
//			IO.println("Renderer.interpolate, s=" + s);
			return Matcher.quoteReplacement(s);
		});
	}

	protected String template(T value) {
		var t = "<!--${}-->";
		if (annotation != null && !annotation.template().isEmpty()) {
			var k1 = annotation.template();
			var k2 = "";
			if (annotation.resource().length == 0) {
				k1 = templateKey1;
				k2 = annotation.template();
			}
			t = renderableFactory.template(k1, k2);
			if (t == null)
				throw new NullPointerException(k1 + ", " + k2);
		}
		return t;
	}
}
