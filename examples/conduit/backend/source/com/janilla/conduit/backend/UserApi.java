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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.janilla.backend.persistence.Persistence;
import com.janilla.ioc.DiFactory;
import com.janilla.java.JavaReflect;
import com.janilla.json.Jwt;
import com.janilla.web.Handle;

@Handle(path = "/api/users")
public class UserApi {

	protected final Properties configuration;

	protected final DiFactory diFactory;

	protected final Persistence persistence;

	public UserApi(Properties configuration, Persistence persistence, DiFactory diFactory) {
		this.configuration = configuration;
		this.persistence = persistence;
		this.diFactory = diFactory;
	}

	@Handle(method = "GET", path = "/api/user")
	public Object getCurrent(User user) {
		var p = user != null ? Map.of("loggedInAs", user.email()) : null;
		var t = p != null
				? Jwt.generateToken(Map.of("alg", "HS256", "typ", "JWT"), p,
						configuration.getProperty("conduit.jwt.key"))
				: null;
		return Collections.singletonMap("user",
				user != null ? new CurrentUser(user.email(), t, user.username(), user.bio(), user.image()) : null);
	}

	@Handle(method = "POST", path = "login")
	public Object authenticate(Authenticate authenticate) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		v.isNotBlank("email", authenticate.user.email);
		v.isNotBlank("password", authenticate.user.password);
		v.orThrow();

		var c = persistence.crud(User.class);
		var u = c.read(c.find("email", new Object[] { authenticate.user.email }));
		{
			var f = HexFormat.of();
			var p = authenticate.user.password.toCharArray();
			var s = u != null ? f.parseHex(u.salt()) : null;
			var h = s != null ? f.formatHex(hash(p, s)) : null;
			v.isValid("email or password", u != null && h.equals(u.hash()));
			v.orThrow();
		}
		return getCurrent(u);
	}

	@Handle(method = "POST")
	public Object register(Register register) {
		var u = register.user;
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (v.isNotBlank("username", u.username) && v.isSafe("username", u.username)) {
			var c = persistence.crud(User.class);
			var x = c.read(c.find("username", new Object[] { u.username }));
			v.hasNotBeenTaken("username", x);
		}
		if (v.isNotBlank("email", u.email) && v.isSafe("email", u.email)) {
			var c = persistence.crud(User.class);
			var x = c.read(c.find("email", new Object[] { u.email }));
			v.hasNotBeenTaken("email", x);
		}
		if (v.isNotBlank("password", u.password))
			v.isSafe("password", u.password);
		v.orThrow();

		if (Boolean.parseBoolean(configuration.getProperty("conduit.live-demo"))) {
			var c = persistence.crud(User.class).count();
			if (c >= 1000)
				throw new ValidationException("existing users", "are too many (" + c + ")");
		}

		var x = new User(null, null, null, null, null, null, null);
		x = JavaReflect.copy(u, x);
		x = setHashAndSalt(x, u.password);
		if (x.image() == null || x.image().isBlank())
			x = new User(x.id(), x.email(), x.hash(), x.salt(), x.username(), x.bio(),
					"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><text x='2' y='12.5' font-size='12'>"
							+ new String(Character.toChars(0x1F600)) + "</text></svg>");
		x = persistence.crud(User.class).create(x);
		return getCurrent(x);
	}

	@Handle(method = "PUT", path = "/api/user")
	public Object update(Update update, User user) {
//		IO.println("update=" + update);
		var u = update.user;
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		var c = persistence.crud(User.class);
//		if (v.isNotBlank("username", u.username) && v.isSafe("username", u.username)
		if (u.username != null && !u.username.isBlank() && v.isSafe("username", u.username)
				&& !u.username.equals(user.username())) {
			var x = c.read(c.find("username", new Object[] { u.username }));
			v.hasNotBeenTaken("username", x);
		}
		if (v.isNotBlank("email", u.email) && v.isSafe("email", u.email) && !u.email.equals(user.email())) {
			var x = c.read(c.find("email", new Object[] { u.email }));
			v.hasNotBeenTaken("email", x);
		}
		v.isEmoji("image", u.image);
		v.isSafe("bio", u.bio);
		v.isSafe("password", u.password);
		v.orThrow();

		var x = c.update(user.id(), y -> {
			y = JavaReflect.copy(u, y);
			if (u.password != null && !u.password.isBlank())
				y = setHashAndSalt(y, u.password);
			return y;
		});
		return getCurrent(x);
	}

	protected static final Random RANDOM = new SecureRandom();

	static User setHashAndSalt(User user, String password) {
		var s = new byte[16];
		RANDOM.nextBytes(s);
		var h = hash(password.toCharArray(), s);
		var f = HexFormat.of();
		return new User(user.id(), user.email(), f.formatHex(h), f.formatHex(s), user.username(), user.bio(),
				user.image());
	}

	static byte[] hash(char[] password, byte[] salt) {
		var s = new PBEKeySpec(password, salt, 10000, 512);
		try {
			var f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return f.generateSecret(s).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public record CurrentUser(String email, String token, String username, String bio, String image) {
	}

	public record Authenticate(User user) {

		public record User(String email, String password) {
		}
	}

	public record Register(User user) {

		public record User(String username, String email, String password) {
		}
	}

	public record Update(User user) {

		public record User(String image, String username, String bio, String email, String password) {
		}
	}
}
