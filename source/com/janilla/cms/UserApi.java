/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.cms;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.janilla.http.HttpExchange;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.BadRequestException;
import com.janilla.web.Bind;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.UnauthorizedException;

public abstract class UserApi<ID extends Comparable<ID>, R extends UserRole, E extends User<ID, R>>
		extends CollectionApi<ID, E> {

	protected final String jwtKey;

	protected UserApi(Class<E> type, Predicate<HttpExchange> drafts, Persistence persistence, String jwtKey) {
		super(type, drafts, persistence);
		this.jwtKey = jwtKey;
	}

	@Handle(method = "PUT", path = "(\\d+)")
	public E update(ID id, E entity, Boolean draft, Boolean autosave, String password) {
		@SuppressWarnings("unchecked")
		var e = (E) entity.withPassword(password);
		return super.update(id, e, draft, autosave);
	}

	@Handle(method = "POST", path = "login")
	public E login(E user, String password, UserHttpExchange exchange) {
		IO.println("UserApi.login, user=" + user + ", password=" + password);
		if (user == null || user.email() == null || user.email().isBlank() || password == null || password.isBlank())
			throw new BadRequestException("Please correct invalid fields.");
		var u = persistence.database().perform(() -> crud().read(crud().find("email", user.email())), false);
		if (u != null && !u.passwordEquals(password))
			u = null;
		if (u == null)
			throw new UnauthorizedException("The email or password provided is incorrect.");
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, jwtKey);
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "POST", path = "logout")
	public void logout(UserHttpExchange exchange) {
		exchange.setSessionCookie(null);
	}

	@Handle(method = "GET", path = "me")
	public E me(UserHttpExchange exchange) {
		@SuppressWarnings("unchecked")
		var e = (E) exchange.sessionUser();
		return e;
	}

	public E read(String email) {
		return email != null ? persistence.database().perform(() -> {
			var c = persistence.crud(type);
			return c.read(c.find("email", email));
		}, false) : null;
	}

	@Handle(method = "POST", path = "first-register")
	public E firstRegister(E user, String password, String confirmPassword, UserHttpExchange exchange) {
		if (user == null || user.email() == null || user.email().isBlank() || password == null || password.isBlank()
				|| !password.equals(confirmPassword))
			throw new BadRequestException("Please correct invalid fields.");
		var u = persistence.database().perform(() -> {
			if (crud().count() != 0)
				throw new ForbiddenException("You are not allowed to perform this action.");
			@SuppressWarnings("unchecked")
			var e = (E) user.withPassword(password); // .withRoles(Set.of(adminRole()));
			return crud().create(e);
		}, true);
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, jwtKey);
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "POST", path = "forgot-password")
	public void forgotPassword(E user) {
		if (user == null || user.email() == null || user.email().isBlank())
			throw new BadRequestException("Please correct invalid fields.");
		var u = persistence.database().perform(() -> {
			var x = crud().read(crud().find("email", user.email()));
			if (x == null)
				return null;
			var t = UUID.randomUUID().toString().replace("-", "");
			return crud().update(x.id(), y -> {
				@SuppressWarnings("unchecked")
				var e = (E) y.withResetPassword(t, Instant.now().plus(1, ChronoUnit.HOURS));
				return e;
			});
		}, true);
		if (u == null)
			return;
		var h = "https://localhost:8443/admin/reset/" + u.resetPasswordToken();
		var hm = """
				<p>
				  Hello Alice.<br>
				  You are receiving this because you (or someone else) have requested the reset of the password for your account.
				  Please click on the following link, or paste this into your browser to complete the process:<br>
				  <a href="${href}">${href}</a><br>
				  Your friend,<br>
				  Bob
				</p>
				"""
				.replace("${href}", h);
		var m = hm.replaceAll("<.*?>", "");
		IO.println("m=" + m);
//		mail(new Data(OffsetDateTime.now(), "foo.bar@example.com", "baz.qux@example.com", "Reset Your Password",
//				m, hm));
	}

	@Handle(method = "POST", path = "reset-password")
	public E resetPassword(String token, String password, String confirmPassword, UserHttpExchange exchange) {
		if (token == null || token.isBlank() || password == null || password.isBlank()
				|| !password.equals(confirmPassword))
			throw new BadRequestException("Please correct invalid fields.");
		var u = persistence.database().perform(() -> {
			var x = crud().read(crud().find("resetPasswordToken", token));
			if (x != null && !Instant.now().isBefore(x.resetPasswordExpiration()))
				x = null;
			if (x == null)
				throw new ForbiddenException("Token is either invalid or has expired.");
			return crud().update(x.id(), y -> {
				@SuppressWarnings("unchecked")
				var e = (E) y.withResetPassword(null, null).withPassword(password);
				return e;
			});
		}, true);
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, jwtKey);
		exchange.setSessionCookie(t);
		return u;
	}

	@Override
	public E delete(ID id) {
		throw new RuntimeException();
	}

	@Handle(method = "DELETE", path = "(\\d+)")
	public E delete(ID id, UserHttpExchange exchange) {
		var u = exchange.sessionUser();
		var x = super.delete(id);
		if (id == u.id())
			logout(exchange);
		return x;
	}

	@Override
	public List<E> delete(List<ID> ids) {
		throw new RuntimeException();
	}

	@Handle(method = "DELETE")
	public List<E> delete(@Bind("id") List<ID> ids, UserHttpExchange exchange) {
		var u = exchange.sessionUser();
		var x = super.delete(ids);
		if (ids.contains(u.id()))
			logout(exchange);
		return x;
	}

	@Override
	protected Set<String> updateInclude(E entity) {
		var nn = Set.of("salt", "hash");
		return entity.salt() == null
				? Reflection.propertyNames(type).filter(x -> !nn.contains(x)).collect(Collectors.toSet())
				: null;
	}

//	private static void mail(Data d) {
//		SSLContext ssl;
//		try {
//			ssl = SSLContext.getInstance("TLSv1.3");
//			ssl.init(null, null, null);
//		} catch (GeneralSecurityException e) {
//			throw new RuntimeException(e);
//		}
//		try (var ch = SocketChannel.open()) {
//			var isa = new InetSocketAddress(InetAddress.getByName("smtp.example.com"), 465);
//			ch.connect(isa);
//			var se = ssl.createSSLEngine();
//			se.setUseClientMode(true);
//			try (var sch = new SslByteChannel(ch, se)) {
//				var hostname = InetAddress.getLocalHost().getCanonicalHostName();
//				var username = "foo.bar@example.com";
//				var password = "**********";
//				var b = "--=_Part_" + ThreadLocalRandom.current().ints(24, '0', '9' + 1).mapToObj(Character::toString)
//						.collect(Collectors.joining());
//				var dbuf = ByteBuffer.allocateDirect(8 * 1024);
//				var charset = Charset.forName("US-ASCII");
//				var encoder = charset.newEncoder();
//				var decoder = charset.newDecoder();
//				for (var s : new String[] { null, "EHLO " + hostname, "AUTH LOGIN",
//						Base64.getEncoder().encodeToString(username.getBytes()),
//						Base64.getEncoder().encodeToString(password.getBytes()), "MAIL FROM:<" + d.from + ">",
//						"RCPT TO:<" + d.to + ">", "DATA",
//						"Date: " + d.date.format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\nFrom: " + d.from + "\nTo: "
//								+ d.to + "\nMessage-ID: <"
//								+ ThreadLocalRandom.current().ints(25, '0', '9' + 1).mapToObj(Character::toString)
//										.collect(Collectors.joining(""))
//								+ "@" + hostname + ">\nSubject: " + d.subject
//								+ "\nMIME-Version: 1.0\nContent-Type: multipart/alternative; boundary=\"" + b + "\"\n--"
//								+ b
//								+ "\nContent-Type: text/plain; charset=us-ascii\nContent-Transfer-Encoding: 7bit\n\n"
//								+ d.message + "--" + b
//								+ "\nContent-Type: text/html; charset=utf-8\nContent-Transfer-Encoding: 7bit\n\n"
//								+ d.htmlMessage + "--" + b + "--\n.",
//						"QUIT" }) {
//					if (s != null) {
//						IO.println("C: " + s.replace("\n", "\nC: "));
//						sch.write(encoder.encode(CharBuffer.wrap(s.replace("\n", "\r\n") + "\r\n")));
//					}
//					dbuf.clear();
//					sch.read(dbuf);
//					dbuf.flip();
//					var cb = decoder.decode(dbuf);
//					IO.println("\t" + cb.toString().replace("\n", "\n\t"));
//				}
//			}
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
//
//	private record Data(OffsetDateTime date, String from, String to, String subject, String message,
//			String htmlMessage) {
//	}
}
