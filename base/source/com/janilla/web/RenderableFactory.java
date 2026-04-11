package com.janilla.web;

import java.lang.reflect.AnnotatedElement;

public interface RenderableFactory {

	String template(String key1, String key2);

	<T> Renderable<T> createRenderable(AnnotatedElement annotated, T value);
}
