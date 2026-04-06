package com.janilla.todomvc.frontend;

import java.util.Map;

import com.janilla.frontend.App;

record AppImpl() implements App {

	@Override
	public String apiUrl() {
		return null;
	}

	@Override
	public Map<String, Object> state() {
		return null;
	}
}
