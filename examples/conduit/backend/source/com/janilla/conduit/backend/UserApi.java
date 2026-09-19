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
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.web.BackendConfig;
import com.janilla.blanktemplate.BlankDomain;
import com.janilla.blanktemplate.backend.BlankUserApi;
import com.janilla.cms.User;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Scope;
import com.janilla.java.Converter;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.json.Jwt;
import com.janilla.web.Handle;

class UserApi extends BlankUserApi {

	protected static final Random RANDOM = new SecureRandom();

	protected final Converter converter;

	protected final DiFactory diFactory;

	UserApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier, Direction defaultDirection,
			Integer defaultDepth, BackendConfig config, BlankDomain domain, Converter converter, DiFactory diFactory) {
		super(drafts, persistence, copier, defaultDirection, defaultDepth, config, domain);
		this.converter = converter;
		this.diFactory = diFactory;
	}

	@Handle(method = "GET", path = "/api/user")
	public Single getCurrent() {
		return new Single(currentUser(user()));
	}

	@Handle(method = "POST", path = "login")
	@Scope("site")
	public Single authenticate(Authenticate authenticate) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		v.isNotBlank("email", authenticate.user.email);
		v.isNotBlank("password", authenticate.user.password);
		v.orThrow();

		var u = crud().read(crud().find("email", new Object[] { authenticate.user.email }));
		{
			var f = HexFormat.of();
			var p = authenticate.user.password.toCharArray();
			var s = u != null ? f.parseHex(u.salt()) : null;
			var h = s != null ? f.formatHex(hash(p, s)) : null;
			v.isValid("email or password", u != null && h.equals(u.hash()));
			v.orThrow();
		}

		return new Single(currentUser(u));
	}

	@Handle(method = "POST")
	public Single register(Register register) {
		var u = register.user;
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (v.isNotBlank("username", u.username) && v.isSafe("username", u.username)) {
			var x = userCrud().read(userCrud().find("username", new Object[] { u.username }));
			v.hasNotBeenTaken("username", x);
		}
		if (v.isNotBlank("email", u.email) && v.isSafe("email", u.email)) {
			var x = userCrud().read(userCrud().find("email", new Object[] { u.email }));
			v.hasNotBeenTaken("email", x);
		}
		if (v.isNotBlank("password", u.password))
			v.isSafe("password", u.password);
		v.orThrow();

		if (config.liveDemo()) {
			var c = userCrud().count();
			if (c >= 1000)
				throw new ValidationException("existing users", "are too many (" + c + ")");
		}

		@SuppressWarnings("unchecked")
		var x = (User<Long>) converter.convert(u, diFactory.classFor(User.class));
		x = setHashAndSalt(x, u.password);
//		if (x.image() == null || x.image().isBlank())
		x = copier.copy(Map.of("image",
				"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><text x='2' y='12.5' font-size='12'>"
						+ new String(Character.toChars(0x1F600)) + "</text></svg>"),
				x);
		x = userCrud().create(x);

		return new Single(currentUser(x));
	}

	@Handle(method = "PUT", path = "/api/user")
	public Single update(Update update) {
//		IO.println("update=" + update);
		var u = update.user;
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (u.username != null && !u.username.isBlank() && v.isSafe("username", u.username)
				&& !u.username.equals(user().name())) {
			var x = userCrud().read(userCrud().find("username", new Object[] { u.username }));
			v.hasNotBeenTaken("username", x);
		}
		if (v.isNotBlank("email", u.email) && v.isSafe("email", u.email) && !u.email.equals(user().email())) {
			var x = userCrud().read(userCrud().find("email", new Object[] { u.email }));
			v.hasNotBeenTaken("email", x);
		}
		v.isEmoji("image", u.image);
		v.isSafe("bio", u.bio);
		v.isSafe("password", u.password);
		v.orThrow();

		var x = userCrud().update((Long) user().id(), y -> {
			y = copier.copy(u, y);
			if (u.password != null && !u.password.isBlank())
				y = setHashAndSalt(y, u.password);
			return y;
		});

		return new Single(currentUser(x));
	}

	protected CurrentUser currentUser(User<?> user) {
		if (user == null)
			return null;

		var p = Map.of("loggedInAs", user.email());
		var t = Jwt.generateToken(Map.of("alg", "HS256", "typ", "JWT"), p, config.jwt().key());
		var cu = converter.convert(Map.of("token", t), CurrentUser.class);
		cu = copier.copy(user, cu);
		return cu;
	}

	protected byte[] hash(char[] password, byte[] salt) {
		var s = new PBEKeySpec(password, salt, 10000, 512);
		try {
			var f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return f.generateSecret(s).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected <ID extends Comparable<ID>> User<ID> setHashAndSalt(User<ID> user, String password) {
		var s = new byte[16];
		RANDOM.nextBytes(s);
		var h = hash(password.toCharArray(), s);
		var f = HexFormat.of();
		return copier.copy(Map.of("hash", f.formatHex(h), "salt", f.formatHex(s)), user);
	}

	protected User<?> user() {
		return ((UserHttpExchange<?>) HttpExchange.SCOPED.get()).sessionUser();
	}

	@SuppressWarnings("unchecked")
	protected UserCrud userCrud() {
		return (UserCrud) persistence.crud(User.class);
	}

	public record Authenticate(User user) {

		public record User(String email, String password) {
		}
	}

	public record CurrentUser(String email, String token, String username, String bio, String image) {
	}

	public record Register(User user) {

		public record User(String username, String email, String password) {
		}
	}

	public record Single(CurrentUser user) {
	}

	public record Update(User user) {

		public record User(String image, String username, String bio, String email, String password) {
		}
	}
}
