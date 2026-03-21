package com.janilla.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public record AnnotationAndElement<T extends Annotation>(T annotation, AnnotatedElement annotated) {
}
