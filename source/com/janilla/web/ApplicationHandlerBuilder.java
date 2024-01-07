package com.janilla.web;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.http.HttpHandler;
import com.janilla.util.Lazy;
import com.janilla.util.Util;

public class ApplicationHandlerBuilder {

//	protected Class<?> type;
//
//	public void setType(Class<?> type) {
//		this.type = type;
//	}

	protected Object application;

	public void setApplication(Object application) {
		this.application = application;
	}

	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
		var f1 = buildMethodHandlerFactory();
		var f2 = buildTemplateHandlerFactory();
		var f3 = buildJsonHandlerFactory();
		var f4 = buildExceptionHandlerFactory();
		var f = new DelegatingHandlerFactory();
		var a = Stream.of(f1, f2, f3, f4).filter(Objects::nonNull).toArray(HandlerFactory[]::new);
		f.setToHandler(o -> {
			for (var g : a) {
				var h = g.createHandler(o);
				if (h != null)
					return h;
			}
			return null;
		});
		if (f1 != null)
			f1.setRenderFactory(f);
		return f;
	});

	public HttpHandler build() {
		return c -> {
			var o = c.getException() != null ? c.getException() : c.getRequest();
			var h = handlerFactory.get().createHandler(o);
			if (h == null)
				throw new NotFoundException();
			h.handle(c);
		};
	}

	protected MethodHandlerFactory buildMethodHandlerFactory() {
		var f = new MethodHandlerFactory();

		var i = new AnnotationDrivenToInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				if (c == application.getClass())
					return application;
				return super.getInstance(c);
			}
		};
		i.setTypes(() -> Util.getPackageClasses(application.getClass().getPackageName()));
		f.setToInvocation(i);

		return f;
	}

	protected TemplateHandlerFactory buildTemplateHandlerFactory() {
		var f = new TemplateHandlerFactory();

		var s = new ToTemplateReader.Simple();
		s.setResourceClass(application.getClass());
		s.setTypes(() -> Util.getPackageClasses(application.getClass().getPackageName()));
		f.setToReader(s);

		return f;
	}

	protected JsonHandlerFactory buildJsonHandlerFactory() {
		return new JsonHandlerFactory();
	}

	protected ExceptionHandlerFactory buildExceptionHandlerFactory() {
		return new ExceptionHandlerFactory();
	}
}
