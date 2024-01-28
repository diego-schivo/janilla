package com.janilla.frontend;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.io.IO;

public record Interpolator(String[] tokens) implements IO.Function<IO.Function<Object, Object>, String> {

	public static IO.Function<IO.Function<Object, Object>, String> of(String template) {
		var b = Stream.<String>builder();
		var i1 = 0;
		for (;;) {
			var i2a = template.indexOf("${", i1);
			var i2b = template.indexOf("#{", i1);
			var i2 = i2a >= 0 && (i2b < 0 || i2a <= i2b) ? i2a : i2b;
			if (i2 < 0) {
				b.add(template.substring(i1));
				break;
			}
			b.add(template.substring(i1, i2));
			i1 = i2 + 2;
			i2 = template.indexOf("}", i1);
			b.add(template.substring(i1, i2));
			i1 = i2 + 1;
		}
		var t = b.build().toArray(String[]::new);
		return new Interpolator(t);
	}

	@Override
	public String apply(IO.Function<Object, Object> evaluator) throws IOException {
		var b = Stream.<String>builder();
		var e = false;
		for (var t : tokens) {
			b.add(e ? Objects.toString(evaluator.apply(t), "") : t);
			e = !e;
		}
		return b.build().collect(Collectors.joining(""));
	}
}
