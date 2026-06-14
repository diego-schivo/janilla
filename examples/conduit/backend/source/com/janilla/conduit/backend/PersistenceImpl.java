package com.janilla.conduit.backend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.blanktemplate.backend.BlankPersistence;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.User;
import com.janilla.conduit.Article;
import com.janilla.conduit.Comment;
import com.janilla.conduit.ConduitDomain;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Copier;
import com.janilla.java.Property;
import com.janilla.persistence.Entity;

class PersistenceImpl extends BlankPersistence<ConduitBackendConfig> {

	private static final Logger LOGGER = System.getLogger(PersistenceImpl.class.getName());

	protected final ConduitDomain domain;

	PersistenceImpl(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, DiFactory diFactory,
			ConduitBackendConfig config, Class<?> seedDataClass, Copier copier, ConduitDomain domain) {
		super(database, storables, diFactory, config, seedDataClass, copier);
		this.domain = domain;
	}

	@Override
	protected void createStoresAndIndexes() {
		database.perform(() -> {
			super.createStoresAndIndexes();

			database.createTable("Article.favoriteList",
					new TableColumn[] { new TableColumn("user", "NUMERIC", false),
							new TableColumn("createdAt", "TEXT", false), new TableColumn("id", "NUMERIC", false) },
					true);

			database.createTable("User.favoriteList",
					new TableColumn[] { new TableColumn("id", "NUMERIC", false),
							new TableColumn("createdAt", "TEXT", false), new TableColumn("user", "NUMERIC", false) },
					true);

			database.createTable("User.followList", new TableColumn[] { new TableColumn("user", "NUMERIC", false),
					new TableColumn("profile", "NUMERIC", false) }, true);

			database.createTable("TagCount", new TableColumn[] { new TableColumn("tag", "TEXT", false),
					new TableColumn("count", "NUMERIC", false) }, true);

			database.createTable("CountTag", new TableColumn[] { new TableColumn("count", "NUMERIC", false),
					new TableColumn("tag", "TEXT", false) }, true);

			return null;
		}, true);
	}

	@Override
	protected Object seedData() {
		var d = (SeedData) super.seedData();

		var r = ThreadLocalRandom.current();
		var ww = new ArrayList<>(Validation.SAFE_WORDS);
		var tt = Randomize.elements(5, 15, ww).distinct().toList();
		var uu = new ArrayList<User<?>>(d.users());
		var aa = new ArrayList<Article>(d.articles());
		for (var i = r.nextInt(6, 11); i > 0; i--) {
			var n = Randomize.phrase(2, 2, () -> Randomize.capitalizeFirstChar(Randomize.element(ww)));
			User<Long> u = diFactory.newInstance(diFactory.classFor(User.class), Map.of("id", 1L + uu.size(), "email",
					n.toLowerCase().replace(' ', '.') + "@lorem.ipsum", "username", n, "image",
					"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><text x='2' y='12.5' font-size='12'>"
							+ new String(Character.toChars(0x1F600 + r.nextInt(0x50))) + "</text></svg>"));
			u = domain.withPassword(u, n.toLowerCase().substring(0, n.indexOf(' ')));
			uu.add(u);

			for (var j = r.nextInt(0, 5); j > 0; j--) {
				var t = Randomize.capitalizeFirstChar(Randomize.phrase(2, 6, () -> Randomize.element(ww)));
				var c = Randomize.instant(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
						OffsetDateTime.now(ZoneOffset.UTC).toInstant());
				var a = new Article(1L + aa.size(), t.toLowerCase().replace(' ', '-'), t,
						Randomize.sentence(3, 10, () -> Randomize.element(ww)), randomMarkdown(ww),
						Randomize.elements(1, 5, tt).distinct().toList(), u, c, c, DocumentStatus.PUBLISHED, c);
				aa.add(a);
			}
		}

		var cc = new ArrayList<Comment>(d.comments());
		for (var i = r.nextInt(20, 31); i > 0; i--) {
			var u = uu.get(r.nextInt(uu.size()));
			var a = aa.get(r.nextInt(aa.size()));
			var j = r.nextInt(1, 8);
			if ((j & 1) != 0) {
				var m = Randomize.instant(a.createdAt(), OffsetDateTime.now(ZoneOffset.UTC).toInstant());
				var c = new Comment(null, Randomize.sentence(3, 10, () -> Randomize.element(ww)), u, a, m, m,
						DocumentStatus.PUBLISHED, m);
				cc.add(c);
			}
			if (a.author().id().equals(u.id()))
				continue;
			if ((j & 2) != 0)
				((ArticleCrud) crud(Article.class)).favorite(a.id(), a.createdAt(), (Long) u.id());
			if ((j & 4) != 0)
				userCrud().follow((Long) a.author().id(), (Long) u.id());
		}

		d = copier.copy(Map.of("articles", aa, "comments", cc, "users", uu), d);
		LOGGER.log(Level.DEBUG, "d={0}", d);

		return d;
	}

	@Override
	protected List<Property> seedProperties() {
		var pp = super.seedProperties();

		var ii = Stream.of("users", "articles").mapToInt(
				n -> IntStream.range(0, pp.size()).filter(i -> pp.get(i).name().equals(n)).findFirst().orElseThrow())
				.toArray();
		var c = Arrays.stream(ii).mapToObj(pp::get).toList();
		pp.removeAll(c);

		var i = Arrays.stream(ii).min().getAsInt();
		pp.addAll(i, c);

		return pp;
	}

	protected String randomMarkdown(List<String> words) {
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

	@SuppressWarnings("unchecked")
	UserCrud userCrud() {
		return (UserCrud) crud(User.class);
	}
}
