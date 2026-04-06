/*
 * MIT License
 *
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
package com.janilla.conduit.backend;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Validation {

	protected final Properties configuration;

	protected final Map<String, Set<String>> errors = new LinkedHashMap<>();

	public Validation(Properties configuration) {
		this.configuration = configuration;
	}

	public boolean hasNotBeenTaken(String name, Object value) {
		if (value == null)
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>()).add("has already been taken");
		return false;
	}

	public boolean isNotBlank(String name, String value) {
		if (value != null && !value.isBlank())
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>()).add("can't be blank");
		return false;
	}

	public boolean isNotTooLong(String name, String value, int maxLength) {
		if ((value != null ? value.length() : 0) <= maxLength)
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>()).add("can't exceed " + maxLength + " characters");
		return false;
	}

	protected static final Pattern NON_WORD = Pattern.compile("[^\\p{L}]+", Pattern.UNICODE_CHARACTER_CLASS);

	protected static final Set<String> SAFE_WORDS = NON_WORD.splitAsStream(
			"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
					+ " Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
					+ " Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
					+ " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
			.map(String::toLowerCase).sorted().collect(Collectors.toCollection(LinkedHashSet::new));

	public boolean isSafe(String name, String value) {
		if (!Boolean.parseBoolean(configuration.getProperty("conduit.live-demo")))
			return true;
		if (value == null || value.isEmpty()
				|| NON_WORD.splitAsStream(value).allMatch(w -> w.isEmpty() || SAFE_WORDS.contains(w.toLowerCase())))
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>())
				.add("can only contain \"Lorem ipsum\" words: " + SAFE_WORDS);
		return false;
	}

	public boolean isUnique(String name, Object value) {
		if (value == null)
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>()).add("must be unique");
		return false;
	}

	public boolean isValid(String name, boolean value) {
		if (value)
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>()).add("is invalid");
		return false;
	}

	public boolean orThrow() {
		if (errors.isEmpty())
			return true;
//		IO.println("errors=" + errors);
		throw new ValidationException(errors);
	}

	protected static final String EMOJI_PREFIX = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><text x='2' y='12.5' font-size='12'>";

	protected static final String EMOJI_SUFFIX = "</text></svg>";

	protected static final Pattern OTHER_SYMBOL = Pattern.compile("\\p{So}", Pattern.UNICODE_CHARACTER_CLASS);

	public boolean isEmoji(String name, String value) {
		if (!Boolean.parseBoolean(configuration.getProperty("conduit.live-demo")))
			return true;
		if (value == null || value.isEmpty()
				|| (value.startsWith(EMOJI_PREFIX) && value.endsWith(EMOJI_SUFFIX)
						&& OTHER_SYMBOL
								.matcher(value.substring(EMOJI_PREFIX.length(), value.length() - EMOJI_SUFFIX.length()))
								.matches()))
			return true;
		errors.computeIfAbsent(name, _ -> new LinkedHashSet<>())
				.add("must be an emoji: " + EMOJI_PREFIX + new String(Character.toChars(0x1F600)) + EMOJI_SUFFIX);
		return false;
	}
}
