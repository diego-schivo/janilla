package com.janilla.java;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaInvoke {

	public static MethodHandle methodHandle(Method method) {
//		IO.println("Reflection.methodHandle, method=" + method);
		class A {
			private static final Map<Method, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(method, _ -> {
			try {
				return MethodHandles.publicLookup().unreflect(method);
			} catch (ReflectiveOperationException e) {
				return new RuntimeException(e);
			}
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (MethodHandle) o;
	}

	public static MethodHandle methodHandle(Constructor<?> constructor) {
//		IO.println("Reflection.methodHandle, constructor=" + constructor);
		class A {
			private static final Map<Constructor<?>, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(constructor, _ -> {
			try {
				var c = constructor.getDeclaringClass();
				Lookup l;
				if (Modifier.isPublic(c.getModifiers()))
					l = MethodHandles.publicLookup();
				else {
					var m1 = JavaReflect.class.getModule();
					var m2 = c.getModule();
					if (!m1.canRead(m2))
						m1.addReads(m2);
					l = MethodHandles.privateLookupIn(c, MethodHandles.lookup());
				}
				return l.unreflectConstructor(constructor);
			} catch (ReflectiveOperationException e) {
				return new RuntimeException(e);
			}
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (MethodHandle) o;
	}
}
