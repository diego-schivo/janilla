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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.cms.UserHttpExchange;
import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.conduit.Article;
import com.janilla.conduit.Comment;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Scope;
import com.janilla.java.Converter;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.java.Flatten;
import com.janilla.persistence.ListPortion;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;

@Handle(path = "/api/articles")
class ArticleApi extends AbstractCollectionApi<Long, Article> {

	protected static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]+",
			Pattern.UNICODE_CHARACTER_CLASS);

	protected final ConduitBackendConfig config;

	protected final Converter converter;

	protected final DiFactory diFactory;

	ArticleApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier, Converter converter,
			ConduitBackendConfig config, DiFactory diFactory) {
		super(Article.class, drafts, persistence, null, copier, Direction.BACKWARD, 1);
		this.converter = converter;
		this.config = config;
		this.diFactory = diFactory;
	}

	@Handle(method = "POST")
	public Single create(Form form) {
		var s = toSlug(form.article.title);
		validate(null, s, form.article);

		if (config.liveDemo()) {
			var c = crud().count();
			if (c >= 1000)
				throw new ValidationException("existing articles", "are too many (" + c + ")");
		}

		var a = converter.convert(Map.of("slug", s, "author", user()), Article.class);
		a = copier.copy(form.article, a);
		a = crud().create(a);

		return new Single(item(a));
	}

	@Handle(method = "POST", path = "([^/]+)/comments")
	public SingleComment createComment(String slug, CommentForm form) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (v.isNotBlank("body", form.comment.body))
			v.isSafe("body", form.comment.body);
		v.orThrow();

		if (config.liveDemo()) {
			var c = commentCrud().count();
			if (c >= 1000)
				throw new ValidationException("existing comments", "are too many (" + c + ")");
		}

		var a = crud().read(crud().find("slug", new Object[] { slug }));
		if (a == null)
			throw new RuntimeException();

		var c = converter.convert(Map.of("article", a, "author", user()), Comment.class);
		c = copier.copy(form.comment, c);
		c = commentCrud().create(c);

		return new SingleComment(c);
	}

	@Handle(method = "DELETE", path = "([^/]+)")
	public void delete(String slug) {
		crud().delete(crud().find("slug", new Object[] { slug }));
	}

	@Handle(method = "DELETE", path = "([^/]+)/comments/([^/]+)")
	public void deleteComment(String slug, Long id) {
		var c = commentCrud().read(id);
		if (!c.author().id().equals(user().id()))
			throw new ForbiddenException();

		commentCrud().delete(id);
	}

	@Handle(method = "POST", path = "([^/]+)/favorite")
	public Single favorite(String slug) {
		var u = user();
		if (u == null)
			throw new NullPointerException("user=" + u);

		var a = crud().read(crud().find("slug", new Object[] { slug }), defaultDepth);
		crud().favorite(a.id(), a.createdAt(), (Long) u.id());

		return new Single(item(a));
	}

	@Handle(method = "GET")
	@Scope("site")
	public Multiple read(String tag, String author, String favorited, Direction direction, Long skip, Long limit,
			Integer depth) {
		var d = direction != null ? direction : defaultDirection;
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var p = depth != null ? depth : defaultDepth;

		ListPortion<Long> ii;
		if (tag != null && !tag.isBlank())
			ii = crud().filterAndCount("tagList", new Object[] { tag }, d, k, l);
		else if (author != null && !author.isBlank()) {
			var a = userCrud().find("username", new Object[] { author });
			ii = crud().filterAndCount("author", new Object[] { a }, d, k, l);
		} else if (favorited != null && !favorited.isBlank()) {
			var f = userCrud().find("username", new Object[] { favorited });
			ii = crud().filterAndCount("favoriteList", new Object[] { f }, d, k, l);
		} else
			ii = crud().filterAndCount("createdAt", new Object[0], d, k, l);

		return new Multiple(ii.elements().stream().map(i -> {
			var a = crud().read(i, p);
			return item(a);
		}).toList(), ii.totalSize());
	}

	@Handle(method = "GET", path = "([^/]+)")
	@Scope("site")
	public Single read(String slug, Integer depth) {
		var p = depth != null ? depth : defaultDepth;
		var a = crud().read(crud().find("slug", new Object[] { slug }), p);

		return new Single(item(a));
	}

	@Handle(method = "GET", path = "([^/]+)/comments")
	public MultipleComments readComments(String slug) {
		var a = crud().find("slug", new Object[] { slug });
		var cc = commentCrud().read(commentCrud().filter("article", new Object[] { a }), 1);

		return new MultipleComments(cc);
	}

	@Handle(method = "GET", path = "feed")
	public Multiple readFeed(Direction direction, Long skip, Long limit, Integer depth) {
		var d = direction != null ? direction : defaultDirection;
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var p = depth != null ? depth : defaultDepth;

		var uu = userCrud().filter("followList", new Object[] { user().id() });
		var ii = !uu.isEmpty() ? crud().filterAndCount("author", uu.toArray(), d, k, l) : ListPortion.<Long>empty();

		return new Multiple(ii.elements().stream().map(i -> {
			var a = crud().read(i, p);
			return item(a);
		}).toList(), ii.totalSize());
	}

	@Handle(method = "DELETE", path = "([^/]+)/favorite")
	public Single unfavorite(String slug) {
		var u = user();
		if (u == null)
			throw new NullPointerException("user=" + u);

		var a = crud().read(crud().find("slug", new Object[] { slug }), defaultDepth);
		crud().unfavorite(a.id(), a.createdAt(), (Long) u.id());

		return new Single(item(a));
	}

	@Handle(method = "PUT", path = "([^/]+)")
	public Single update(String slug, Form form) {
		var s = Objects.requireNonNullElse(toSlug(form.article.title), slug);
		validate(slug, s, form.article);

		var a = crud().update(crud().find("slug", new Object[] { slug }), x -> copier.copy(form.article, x));

		return new Single(item(a));
	}

	protected Crud<Long, Comment> commentCrud() {
		return persistence.crud(Comment.class);
	}

	@Override
	protected ArticleCrud crud() {
		return (ArticleCrud) super.crud();
	}

	protected Item item(Article article) {
		if (article == null)
			return null;

		var u = user();
		var f = u != null && article.id() != null && persistence.crud(Article.class)
				.filter("favoriteList", new Object[] { u.id() }).stream().anyMatch(x -> x.equals(article.id()));
		var fc = article.id() != null ? userCrud().count("favoriteList", new Object[] { article.id() }) : 0;
		return new Item(article, f, fc);
	}

	protected String toSlug(String title) {
		return title != null ? NON_ALPHANUMERIC.splitAsStream(title.toLowerCase()).collect(Collectors.joining("-"))
				: null;
	}

	protected User<?> user() {
		return ((UserHttpExchange<?>) HttpExchange.SCOPED.get()).sessionUser();
	}

	@SuppressWarnings("unchecked")
	protected UserCrud userCrud() {
		return (UserCrud) persistence.crud(User.class);
	}

	protected void validate(String slug1, String slug2, Form.Article article) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		var c = crud();
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

	public record CommentForm(Comment comment) {

		public record Comment(String body) {
		}
	}

	public record Form(Article article) {

		public record Article(String title, String description, String body, List<String> tagList) {
		}
	}

	public record Item(@Flatten Article article, boolean favorited, long favoritesCount) {
	}

	public record Multiple(List<Item> articles, long articlesCount) {
	}

	public record MultipleComments(List<Comment> comments) {
	}

	public record Single(Item article) {
	}

	public record SingleComment(Comment comment) {
	}
}
