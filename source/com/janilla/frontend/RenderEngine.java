package com.janilla.frontend;

import java.io.IOException;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.io.IO;
import com.janilla.reflect.Reflection;
import com.janilla.web.Render;

public class RenderEngine {

	public static void main(String[] args) throws IOException {
	}

	IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator;

	public void setToInterpolator(
			IO.Function<String, IO.Function<IO.Function<Object, Object>, String>> toInterpolator) {
		this.toInterpolator = toInterpolator;
	}

	public Object render(ObjectAndType input) throws IOException {
		if (input.object == null)
			return null;
		var r = input.type != null ? input.type.getAnnotation(Render.class) : null;
		if (r == null)
			r = input.object.getClass().getAnnotation(Render.class);
		var i = switch (input.object) {
		case Collection<?> l -> l.iterator();
		case Stream<?> s -> s.iterator();
		default -> null;
		};
		if (i != null) {
			var t = ((AnnotatedParameterizedType) input.type).getAnnotatedActualTypeArguments()[0];
			var b = Stream.<String>builder();
			var d = r != null ? r.delimiter() : null;
			while (i.hasNext()) {
				b.add(render(new ObjectAndType(i.next(), t)).toString());
				if (i.hasNext() && r != null && !d.isEmpty())
					b.add(d);
			}
			input = new ObjectAndType(b.build().collect(Collectors.joining()), input.type);
		}

		var t = r != null ? r.template() : null;
		var j = t != null && !t.isEmpty() ? toInterpolator.apply(t) : null;
		var c = input;
		return j != null ? j.apply(x -> evaluate((String) x, c)) : input.object;
	}

	Object evaluate(String expression, ObjectAndType context) throws IOException {
		var o = context.object;
		if (o == null)
			return null;
		if (expression.isEmpty())
			return o;
		var i = expression.indexOf('.');
		var n = i >= 0 ? expression.substring(0, i) : expression;
		ObjectAndType p;
		switch (o) {
		case Map<?, ?> m: {
			var v = m.get(n);
			var t = ((AnnotatedParameterizedType) context.type).getAnnotatedActualTypeArguments()[1];
			p = new ObjectAndType(v, t);
			break;
		}
		case Function<?, ?> f: {
			@SuppressWarnings("unchecked")
			var g = (Function<String, ?>) f;
			var v = g.apply(n);
			var t = ((AnnotatedParameterizedType) context.type).getAnnotatedActualTypeArguments()[1];
			p = new ObjectAndType(v, t);
			break;
		}
		default: {
			var g = Reflection.getter(o.getClass(), n);
			if (g == null)
				throw new NullPointerException(o + " " + n);
			Object v;
			try {
				v = g.invoke(o);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			var t = g.getAnnotatedReturnType();
			p = new ObjectAndType(v, t);
			break;
		}
		}
		if (i >= 0) {
			var v = evaluate(expression.substring(i + 1), p);
			p = new ObjectAndType(v, p.type);
		}
		return render(p);
	}

	public record ObjectAndType(Object object, AnnotatedType type) {
	}
}
