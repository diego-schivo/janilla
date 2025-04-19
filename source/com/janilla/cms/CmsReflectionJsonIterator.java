package com.janilla.cms;

import java.lang.reflect.AnnotatedParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.ReflectionValueIterator;
import com.janilla.json.TokenIterationContext;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class CmsReflectionJsonIterator extends ReflectionJsonIterator {

	protected final Persistence persistence;

	public CmsReflectionJsonIterator(Persistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		var o = stack().peek();
		if (o instanceof Map.Entry<?, ?> kv) {
			var n = (String) kv.getKey();
			if (object instanceof Long l) {
				o = stack().pop();
				var p = Reflection.property(stack().peek().getClass(), n);
				var ta = p != null ? p.annotatedType().getAnnotation(Types.class) : null;
				var t = ta != null ? ta.value()[0] : null;
				if (t != null)
					object = o = persistence.crud(t).read(l);
				stack().push(o);
			} else if (object instanceof Document.Reference<?> r) {
				object = persistence.crud(r.type()).read(r.id());
			} else if (object instanceof List<?> oo && !oo.isEmpty() && oo.getFirst() instanceof Long) {
				o = stack().pop();
				var p = Reflection.property(stack().peek().getClass(), n);
				var apt = p != null && p.annotatedType() instanceof AnnotatedParameterizedType x ? x : null;
				var ta = apt != null ? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class) : null;
				var t = ta != null ? ta.value()[0] : null;
				if (t != null)
					object = o = list(persistence.crud(t).read(oo.stream().mapToLong(x -> (long) x).toArray()));
				stack().push(o);
			}
		}
		return new CustomReflectionValueIterator(this, object);
	}

	protected List<?> list(List<?> list) {
		return list;
	}

	protected class CustomReflectionValueIterator extends ReflectionValueIterator {

		public CustomReflectionValueIterator(TokenIterationContext context, Object object) {
			super(context, object);
		}

		@Override
		protected Iterator<JsonToken<?>> newIterator() {
			return object instanceof Class<?> c
					? context.newStringIterator(c.getName().substring(c.getPackageName().length() + 1))
					: super.newIterator();
		}

		@Override
		protected Stream<Entry<String, Object>> entries(Class<?> class0) {
			var kkvv = super.entries(class0);
			var v = class0.getAnnotation(Versions.class);
			if (v == null || !v.drafts())
				kkvv = kkvv.filter(kv -> kv.getKey() != "status");
			if (v != null) {
				var n = Version.class.getSimpleName() + "<" + class0.getSimpleName() + ">.document";
				var vc = persistence.database()
						.perform((_, ii) -> ii.perform(n, i -> i.count(((Document) object).id())), false);
				var kv = Map.entry("versionCount", (Object) vc);
				kkvv = Stream.concat(kkvv, Stream.of(kv));
			}
			return kkvv;
		}
	}
}
