/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.acmedashboard.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.janilla.java.Converter;
import com.janilla.json.Json;

public record PlaceholderData(List<Customer> customers, List<Invoice> invoices, List<Revenue> revenue,
		List<User> users) {

	public static PlaceholderData read() {
		try (var x = PlaceholderData.class.getResourceAsStream("placeholder-data.json")) {
			return (PlaceholderData) new Converter(null)
//			{
//
//				@Override
//				@SuppressWarnings("unchecked")
//				public <T> T convert(Object object, Type target) {
//					if (object instanceof String s && target == Customer.class)
//						return (T) Customer.EMPTY.withId(UUID.fromString(s));
//					return super.convert(object, target);
//				}
//			}
			.convert(Json.parse(new String(x.readAllBytes())), PlaceholderData.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
