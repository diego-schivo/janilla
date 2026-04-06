/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.backend;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new Lazy<T>(supplier);
	}

	final Supplier<T> supplier;

	final Lock lock = new ReentrantLock();

	volatile boolean got;

	T result;

	private Lazy(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T get() {
		if (!got) {
			lock.lock();
			try {
				if (!got) {
					result = supplier.get();
					got = true;
				}
			} finally {
				lock.unlock();
			}
		}
		return result;
	}
}
