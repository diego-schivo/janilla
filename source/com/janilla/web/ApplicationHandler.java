//package com.janilla.web;
//
//import java.io.IOException;
//import java.util.function.Supplier;
//
//import com.janilla.http.ExchangeContext;
//import com.janilla.http.HttpHandler;
//import com.janilla.util.Lazy;
//import com.janilla.util.Util;
//
//public class ApplicationHandler implements HttpHandler {
//
//	private Class<?> type;
//
//	public Class<?> getType() {
//		return type;
//	}
//
//	public void setType(Class<?> type) {
//		this.type = type;
//	}
//
//	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
//		var f1 = new MethodHandlerFactory();
//		{
//			var i = new AnnotationDrivenToInvocation();
//			i.setTypes(() -> Util.getPackageClasses(type.getPackageName()));
//			f1.setToInvocation(i);
//		}
//
//		var f2 = new TemplateHandlerFactory();
//		{
//			var s = new ToTemplateReader.Simple();
//			s.setResourceClass(type);
////			s.setTypes(() -> Stream.concat(Util.packageClasses("com.janilla.frontend"),
////					Util.packageClasses(type.getPackageName())));
//			s.setTypes(() -> Util.getPackageClasses(type.getPackageName()));
//			f2.setToReader(s);
//		}
//
//		var f = new DelegatingHandlerFactory();
//		{
//			var a = new HandlerFactory[] { f1, f2 };
//			f.setToHandler(o -> {
//				for (var g : a) {
//					var h = g.createHandler(o);
//					if (h != null)
//						return h;
//				}
//				return null;
//			});
//		}
//		f1.setRenderFactory(f);
//		return f;
//	});
//
//	@Override
//	public void handle(ExchangeContext context) throws IOException {
//		var o = context.getException() != null ? context.getException() : context.getRequest();
//		var h = handlerFactory.get().createHandler(o);
//		if (h == null)
//			throw new NotFoundException();
//		h.handle(context);
//	}
//}
