package com.janilla.cms;

import java.util.Map;

import com.janilla.java.Converter;
import com.janilla.java.TypeResolver;

public class CmsConverter extends Converter {

	public CmsConverter(TypeResolver typeResolver) {
		super(typeResolver);
	}

	@Override
	protected Object convertMap(Map<?, ?> map, Class<?> target) {
		if (target == DocumentReference.class) {
			var d = (Document<?>) super.convertMap(map, null);
			return new DocumentReference<>(d.getClass(), d.id());
		}
		return super.convertMap(map, target);
	}
}
