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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.ioc.DiFactory;

public class CustomPersistenceBuilder extends PersistenceBuilder {

	protected final Properties configuration;

	public CustomPersistenceBuilder(Path databaseFile, Properties configuration) {
		super(databaseFile);
		this.configuration = configuration;
	}

	@Override
	public Persistence build(DiFactory diFactory) {
		var e = Files.exists(databaseFile);
		var x = super.build(diFactory);
		if (!e) {
			var s = Boolean.parseBoolean(configuration.getProperty("conduit.database.seed"));
			if (s)
				seed(x);
		}
		return x;
	}

	private void seed(Persistence persistence) {
		var r = ThreadLocalRandom.current();
		var ww = new ArrayList<>(Validation.SAFE_WORDS);
		var tags = Randomize.elements(5, 15, ww).distinct().toList();
		for (var i = r.nextInt(6, 11); i > 0; i--) {
			var n = Randomize.phrase(2, 2, () -> Randomize.capitalizeFirstChar(Randomize.element(ww)));
			var u = new User(null, n.toLowerCase().replace(' ', '.') + "@lorem.ipsum", null, null, n, null,
					"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><text x='2' y='12.5' font-size='12'>"
							+ new String(Character.toChars(0x1F600 + r.nextInt(0x50))) + "</text></svg>");
			u = UserApi.setHashAndSalt(u, n.toLowerCase().substring(0, n.indexOf(' ')));
			u = persistence.crud(User.class).create(u);
			for (var j = r.nextInt(0, 5); j > 0; j--) {
				var t = Randomize.capitalizeFirstChar(Randomize.phrase(2, 6, () -> Randomize.element(ww)));
				var c = Randomize.instant(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
						OffsetDateTime.now(ZoneOffset.UTC).toInstant());
				var a = new Article(null, t.toLowerCase().replace(' ', '-'), t,
						Randomize.sentence(3, 10, () -> Randomize.element(ww)), randomMarkdown(ww),
						Randomize.elements(1, 5, tags).distinct().toList(), c, c, u);
				persistence.crud(Article.class).create(a);
			}
		}
		for (var i = r.nextInt(20, 31); i > 0; i--) {
			var u = persistence.crud(User.class).read(1 + r.nextLong(persistence.crud(User.class).count()));
			var a = persistence.crud(Article.class).read(1 + r.nextLong(persistence.crud(Article.class).count()));
			var j = r.nextInt(1, 8);
			if ((j & 1) != 0) {
				var d = Randomize.instant(a.createdAt(), OffsetDateTime.now(ZoneOffset.UTC).toInstant());
				var c = new Comment(null, d, d, Randomize.sentence(3, 10, () -> Randomize.element(ww)), u, a);
				c = persistence.crud(Comment.class).create(c);
			}
			if (a.author().id().equals(u.id()))
				continue;
			if ((j & 2) != 0)
				((ArticleCrud) persistence.crud(Article.class)).favorite(a.id(), a.createdAt(), u.id());
			if ((j & 4) != 0)
				((UserCrud) persistence.crud(User.class)).follow(a.author().id(), u.id());
		}
	}

	private static String randomMarkdown(List<String> words) {
		var r = ThreadLocalRandom.current();
		var b = Stream.<String>builder();
		if (r.nextBoolean())
			b.add("# " + Randomize.capitalizeFirstChar(Randomize.phrase(3, 7, () -> Randomize.element(words))) + "\n");
		for (var i = r.nextInt(1, 6); i > 0; i--) {
			if (r.nextBoolean())
				b.add("### " + Randomize.capitalizeFirstChar(Randomize.phrase(3, 7, () -> Randomize.element(words)))
						+ "\n");
			if (r.nextInt(3) == 2)
				b.add(Stream.iterate("", _ -> "- " + Randomize.phrase(5, 11, () -> Randomize.element(words))).skip(1)
						.limit(r.nextInt(2, 6)).collect(Collectors.joining("\n")) + "\n");
			else
				b.add(Stream.iterate("", _ -> Randomize.sentence(5, 11, () -> Randomize.element(words))).skip(1)
						.limit(r.nextInt(2, 6)).collect(Collectors.joining(" ")) + "\n");
		}
		return b.build().collect(Collectors.joining("\n"));
	}
}
