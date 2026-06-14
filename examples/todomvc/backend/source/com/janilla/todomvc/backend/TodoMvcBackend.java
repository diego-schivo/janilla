package com.janilla.todomvc.backend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.todomvc.TodoMvcDomain;
import com.janilla.web.WebApp;

public class TodoMvcBackend extends BlankBackend<TodoMvcBackendConfig, TodoMvcDomain> {

	private static final Logger LOGGER = System.getLogger(TodoMvcBackend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankBackend.diTypes(), Java.getPackageTypes("com.janilla.todomvc"),
				Java.getPackageTypes("com.janilla.todomvc.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var cfg = newConfig(new Class<?>[] { TodoMvcBackend.class }, args.length != 0 ? args[0] : null, f);
		var ctx = (Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
		serve(a[0]);
	}

	public TodoMvcBackend(TodoMvcBackendConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context, Data.class, null);
	}
}
