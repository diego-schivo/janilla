package com.janilla.backend.cms;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.janilla.backend.persistence.CrudObserver;
import com.janilla.java.Reflection;

public class DocumentObserver<D extends Document<?>> implements CrudObserver<D> {

	@Override
	public D beforeCreate(D document) {
		var i = Instant.now();
		var v = document.getClass().getAnnotation(Versions.class);
		var m = Map.<String, Object>of("createdAt", i, "updatedAt", i);
		if (document.documentStatus() == null) {
			m = new HashMap<>(m);
			m.put("documentStatus", v != null && v.drafts() ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED);
		}
		return Reflection.copy(m, document);
	}

	@Override
	public D beforeUpdate(D document) {
		var i = Instant.now();
		var m = Map.<String, Object>of("updatedAt", i);
		if (document.documentStatus() == DocumentStatus.PUBLISHED) {
			m = new HashMap<>(m);
			m.put("publishedAt", i);
		}
		return Reflection.copy(m, document);
	}
}
