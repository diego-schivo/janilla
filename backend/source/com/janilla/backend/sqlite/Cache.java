package com.janilla.backend.sqlite;

import java.util.LinkedHashMap;

public class Cache<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = 2913088298650670193L;

	protected final int capacity;

	public Cache(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public V get(Object key) {
//		IO.println("PageCache.get, key=" + key);

		@SuppressWarnings("unchecked")
		var k = (K) key;
		var v = super.get(k);

		if (v != null) {
			remove(k);
			put(k, v);
//			IO.println("PageCache.get, keys=" + keySet());
		}

		return v;
	}

	@Override
	public V put(K key, V value) {
//		IO.println("PageCache.put, key=" + key);

		if (remove(key) == null && size() == capacity) {
			var kk = keySet().iterator();
			var k = kk.next();
//			IO.println("PageCache.put, k=" + k);
			kk.remove();
			remove(k);
		}

		var v = super.put(key, value);
//		IO.println("PageCache.put, keys=" + keySet());
		return v;
	}
}
