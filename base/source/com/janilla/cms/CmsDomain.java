/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.cms;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.janilla.java.Converter;
import com.janilla.java.Copier;
import com.janilla.java.Java;
import com.janilla.web.Domain;

public class CmsDomain implements Domain {

	protected final Converter converter;

	protected final Copier copier;

	protected final Random random = new SecureRandom();

	protected final SecretKeyFactory secret;

	private final Map<String, UserRole> userRoles = new ConcurrentHashMap<>();

	public CmsDomain(Converter converter, Copier copier) {
		this.converter = converter;
		this.copier = copier;

		try {
			secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] hash(char[] password, byte[] salt) {
		var ks = new PBEKeySpec(password, salt, 10000, 512);
		Key k;
		try {
			k = secret.generateSecret(ks);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		return k.getEncoded();
	}

	public boolean passwordEquals(User<?> user, String password) {
		var f = HexFormat.of();
		var s = f.parseHex(user.salt());
		var h = hash(password.toCharArray(), s);
		return f.formatHex(h).equals(user.hash());
	}

	public int userDepth() {
		return 0;
	}

	public UserRole userRole(String name) {
		return userRoles.computeIfAbsent(name, k -> converter.convert(k, UserRole.class));
	}

	public <ID extends Comparable<ID>> User<ID> withPassword(User<ID> user, String password) {
		if (password == null || password.isEmpty())
			return copier.copy(Java.hashMap("salt", null, "hash", null), user);
		var s = new byte[16];
		random.nextBytes(s);
		var h = hash(password.toCharArray(), s);
		var f = HexFormat.of();
		return copier.copy(Java.hashMap("salt", f.formatHex(s), "hash", f.formatHex(h)), user);
	}

	public <ID extends Comparable<ID>> User<ID> withResetPassword(User<ID> user, String resetPasswordToken,
			Instant resetPasswordExpiration) {
		return copier.copy(Java.hashMap("resetPasswordToken", resetPasswordToken, "resetPasswordExpiration",
				resetPasswordExpiration), user);
	}

	public <ID extends Comparable<ID>> User<ID> withRoles(User<ID> user, Set<UserRole> roles) {
		return copier.copy(Java.hashMap("roles", roles), user);
	}
}
