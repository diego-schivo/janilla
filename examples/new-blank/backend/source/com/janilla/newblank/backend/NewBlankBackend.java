package com.janilla.newblank.backend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.newblank.NewBlankDomain;
import com.janilla.web.WebApp;

public class NewBlankBackend extends BlankBackend<NewBlankBackendConfig, NewBlankDomain> {

	private static final Logger LOGGER = System.getLogger(NewBlankBackend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankBackend.diTypes(), Java.getPackageTypes("com.janilla.newblank"),
				Java.getPackageTypes("com.janilla.newblank.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var cfg = newConfig(new Class<?>[] { NewBlankBackend.class }, args.length != 0 ? args[0] : null, f);
		var ctx = (Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
		serve(a[0]);
	}

	public NewBlankBackend(NewBlankBackendConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context, Data.class, SeedData.class);
	}
}
