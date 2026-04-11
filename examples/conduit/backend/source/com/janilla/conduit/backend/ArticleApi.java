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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.janilla.java.Configuration;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.ioc.DiFactory;
import com.janilla.java.JavaReflect;
import com.janilla.persistence.ListPortion;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;

@Handle(path = "/api/articles")
public class ArticleApi {

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final Persistence persistence;

	public ArticleApi(Configuration configuration, Persistence persistence, DiFactory diFactory) {
		this.configuration = configuration;
		this.persistence = persistence;
		this.diFactory = diFactory;
	}

	@Handle(method = "POST")
	public Object create(Form form, User user) {
		var s = toSlug(form.article.title);
		validate(null, s, form.article);

		if (Boolean.parseBoolean(configuration.getProperty("conduit.live-demo"))) {
			var c = persistence.crud(Article.class).count();
			if (c >= 1000)
				throw new ValidationException("existing articles", "are too many (" + c + ")");
		}

		var n = Instant.now();
		var a = new Article(null, s, null, null, null,
//				form.article.tagList() != null ? form.article.tagList().stream().sorted().toList() : List.of(),
				null, n, n, user);
		a = JavaReflect.copy(form.article, a, x -> !Set.of("id", "slug",
//						"tagList",
				"createdAt", "updatedAt", "author").contains(x));
		a = persistence.crud(Article.class).create(a);

		return Map.of("article", a);
	}

	@Handle(method = "GET", path = "([^/]+)")
	public Object read(String slug) {
//		IO.println("ArticleApi.read, slug=" + slug);
		var c = persistence.crud(Article.class);
		var x = c.read(c.find("slug", new Object[] { slug }));
//		IO.println("x=" + x);
		return Collections.singletonMap("article", x);
	}

	@Handle(method = "PUT", path = "([^/]+)")
	public Object update(String slug, Form form, User user) {
		var s = Objects.requireNonNullElse(toSlug(form.article.title), slug);
		validate(slug, s, form.article);

		var c = persistence.crud(Article.class);
		var a = c.update(c.find("slug", new Object[] { slug }), x -> {
			x = new Article(x.id(), s, null, null, null,
//					form.article.tagList() != null ? form.article.tagList().stream().sorted().toList() : List.of(),
					null, x.createdAt(), Instant.now(), x.author());
			return JavaReflect.copy(form.article, x, y -> !Set.of("slug",
//					"tagList",
					"createdAt", "updatedAt", "author").contains(y));
		});

		return Collections.singletonMap("article", a);
	}

	@Handle(method = "DELETE", path = "([^/]+)")
	public void delete(String slug) {
		var c = persistence.crud(Article.class);
		c.delete(c.find("slug", new Object[] { slug }));
	}

	@Handle(method = "GET")
	public Object list(String tag, String author, String favorited, Long skip, Long limit) {
		var c = persistence.crud(Article.class);
		ListPortion<Long> p;
		if (tag != null && !tag.isBlank())
			p = c.filterAndCount("tagList", new Object[] { tag }, true, skip != null ? skip : 0,
					limit != null ? limit : -1);
		else if (author != null && !author.isBlank()) {
			var a = persistence.crud(User.class).find("username", new Object[] { author });
			p = c.filterAndCount("author", new Object[] { a }, true, skip != null ? skip : 0,
					limit != null ? limit : -1);
		} else if (favorited != null && !favorited.isBlank()) {
			var f = persistence.crud(User.class).find("username", new Object[] { favorited });
			p = c.filterAndCount("favoriteList", new Object[] { f }, true, skip != null ? skip : 0,
					limit != null ? limit : -1);
		} else
			p = c.filterAndCount("createdAt", new Object[0], true, skip != null ? skip : 0, limit != null ? limit : -1);
		return Map.of("articles", c.read(p.elements()), "articlesCount", p.totalSize());
	}

	@Handle(method = "GET", path = "feed")
	public Object listFeed(Long skip, Long limit, User user) {
		var u = persistence.crud(User.class).filter("followList", new Object[] { user.id() });
		var c = persistence.crud(Article.class);
		var p = !u.isEmpty()
				? c.filterAndCount("author", u.toArray(), true, skip != null ? skip : 0, limit != null ? limit : -1)
				: ListPortion.<Long>empty();
		return Map.of("articles", c.read(p.elements()), "articlesCount", p.totalSize());
	}

	@Handle(method = "POST", path = "([^/]+)/comments")
	public Object createComment(String slug, CommentForm form, User user) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (v.isNotBlank("body", form.comment.body))
			v.isSafe("body", form.comment.body);
		v.orThrow();

		if (Boolean.parseBoolean(configuration.getProperty("conduit.live-demo"))) {
			var c = persistence.crud(Comment.class).count();
			if (c >= 1000)
				throw new ValidationException("existing comments", "are too many (" + c + ")");
		}

		var a = persistence.crud(Article.class).find("slug", new Object[] { slug });
		if (a == null)
			throw new RuntimeException();

		var n = Instant.now();
		var c = new Comment(null, n, n, null, user, Article.EMPTY.withId(a));
		c = JavaReflect.copy(form.comment, c, x -> x.equals("body"));
		c = persistence.crud(Comment.class).create(c);
		return Map.of("comment", c);
	}

	@Handle(method = "DELETE", path = "([^/]+)/comments/([^/]+)")
	public void deleteComment(String slug, Long id, User user) {
		var c = persistence.crud(Comment.class);
		var x = c.read(id);
		if (x.author().id().equals(user.id()))
			c.delete(id);
		else
			throw new ForbiddenException();
	}

	@Handle(method = "GET", path = "([^/]+)/comments")
	public Object listComments(String slug) {
		var a = persistence.crud(Article.class).find("slug", new Object[] { slug });
		var c = persistence.crud(Comment.class);
		return Map.of("comments", c.read(c.filter("article", new Object[] { a })));
	}

	@Handle(method = "POST", path = "([^/]+)/favorite")
	public Object favorite(String slug, User user) {
		if (user == null)
			throw new NullPointerException("user=" + user);
		var c = (ArticleCrud) persistence.crud(Article.class);
		var a = c.read(c.find("slug", new Object[] { slug }));
		c.favorite(a.id(), a.createdAt(), user.id());
		return Map.of("article", a);
	}

	@Handle(method = "DELETE", path = "([^/]+)/favorite")
	public Object unfavorite(String slug, User user) {
		if (user == null)
			throw new NullPointerException("user=" + user);
		var c = (ArticleCrud) persistence.crud(Article.class);
		var a = c.read(c.find("slug", new Object[] { slug }));
		c.unfavorite(a.id(), a.createdAt(), user.id());
		return Map.of("article", a);
	}

	protected static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]+",
			Pattern.UNICODE_CHARACTER_CLASS);

	protected String toSlug(String title) {
		return title != null ? NON_ALPHANUMERIC.splitAsStream(title.toLowerCase()).collect(Collectors.joining("-"))
				: null;
	}

	protected void validate(String slug1, String slug2, Form.Article article) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		var c = persistence.crud(Article.class);
		if ((slug2.equals(slug1) && article.title == null) || (v.isNotBlank("title", article.title)
				&& v.isNotTooLong("title", article.title, 100) && v.isSafe("title", article.title))) {
			var a = c.read(c.filter("slug", new Object[] { slug2 })).stream().filter(x -> !x.slug().equals(slug1))
					.findFirst().orElse(null);
			v.isUnique("title", a);
		}
		if ((slug2.equals(slug1) && article.description == null) || (v.isNotBlank("description", article.description)
				&& v.isNotTooLong("description", article.description, 200)
				&& v.isSafe("description", article.description)))
			;
		if (v.isNotBlank("body", article.body) && v.isNotTooLong("body", article.body, 2000)
				&& v.isSafe("body", article.body))
			;
		var t = article.tagList != null ? article.tagList.stream().collect(Collectors.joining(" ")) : null;
		if (v.isNotTooLong("tagList", t, 100) && v.isSafe("tagList", t))
			;
		v.orThrow();
	}

//	public record Filter(String tag, String author, String favorited) {
//	}
//
//	public record Range(long skip, long limit) {
//	}

	public record Form(Article article) {

		public record Article(String title, String description, String body, List<String> tagList) {
		}
	}

	public record CommentForm(Comment comment) {

		public record Comment(String body) {
		}
	}
}
