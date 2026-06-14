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
import com.janilla.backend.persistence.Persistence;
import com.janilla.conduit.Article;
import com.janilla.conduit.Comment;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
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
	public Object create(Form form, UserHttpExchange<?> exchange) {
		var s = toSlug(form.article.title);
		validate(null, s, form.article);

		if (config.liveDemo() != null && config.liveDemo()) {
			var c = crud().count();
			if (c >= 1000)
				throw new ValidationException("existing articles", "are too many (" + c + ")");
		}

		Article a = converter.convert(Map.of("slug", s, "author", exchange.sessionUser()), Article.class);
		a = copier.copy(form.article, a);
		a = crud().create(a);

		return Map.of("article", a);
	}

	@Handle(method = "POST", path = "([^/]+)/comments")
	public Object createComment(String slug, CommentForm form, UserHttpExchange<?> exchange) {
		var v = diFactory.newInstance(diFactory.classFor(Validation.class));
		if (v.isNotBlank("body", form.comment.body))
			v.isSafe("body", form.comment.body);
		v.orThrow();

		if (config.liveDemo() != null && config.liveDemo()) {
			var c = persistence.crud(Comment.class).count();
			if (c >= 1000)
				throw new ValidationException("existing comments", "are too many (" + c + ")");
		}

		var a = crud().read(crud().find("slug", new Object[] { slug }));
		if (a == null)
			throw new RuntimeException();

		Comment c = converter.convert(Map.of("article", a, "author", exchange.sessionUser()), Comment.class);
		c = copier.copy(form.comment, c);
		c = persistence.crud(Comment.class).create(c);

		return Map.of("comment", c);
	}

	@Handle(method = "DELETE", path = "([^/]+)")
	public void delete(String slug) {
		crud().delete(crud().find("slug", new Object[] { slug }));
	}

	@Handle(method = "DELETE", path = "([^/]+)/comments/([^/]+)")
	public void deleteComment(String slug, Long id, UserHttpExchange<?> exchange) {
		var c = persistence.crud(Comment.class);
		var x = c.read(id);
		if (x.author().id().equals(exchange.sessionUser().id()))
			c.delete(id);
		else
			throw new ForbiddenException();
	}

	@Handle(method = "POST", path = "([^/]+)/favorite")
	public Object favorite(String slug, UserHttpExchange<?> exchange) {
		var u = exchange.sessionUser();
		if (u == null)
			throw new NullPointerException("user=" + u);
		var c = (ArticleCrud) crud();
		var a = c.read(c.find("slug", new Object[] { slug }));
		c.favorite(a.id(), a.createdAt(), (Long) u.id());
		return Map.of("article", a);
	}

	@Handle(method = "GET")
	public ListPortion<Article> read(String tag, String author, String favorited, Direction direction, Long skip,
			Long limit, Integer depth) {
		var d = direction != null ? direction : defaultDirection;
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var p = depth != null ? depth : defaultDepth;

		ListPortion<Long> ii;
		if (tag != null && !tag.isBlank())
			ii = crud().filterAndCount("tagList", new Object[] { tag }, d, k, l);
		else if (author != null && !author.isBlank()) {
			var a = ((PersistenceImpl) persistence).userCrud().find("username", new Object[] { author });
			ii = crud().filterAndCount("author", new Object[] { a }, d, k, l);
		} else if (favorited != null && !favorited.isBlank()) {
			var f = ((PersistenceImpl) persistence).userCrud().find("username", new Object[] { favorited });
			ii = crud().filterAndCount("favoriteList", new Object[] { f }, d, k, l);
		} else
			ii = crud().filterAndCount("createdAt", new Object[0], d, k, l);

		return ii.map(x -> crud().read(x, p));
	}

	@Handle(method = "GET", path = "([^/]+)")
	public Article read(String slug, Integer depth) {
		return crud().read(crud().find("slug", new Object[] { slug }), depth != null ? depth : defaultDepth);
	}

	@Handle(method = "GET", path = "([^/]+)/comments")
	public Object readComments(String slug) {
		var a = crud().find("slug", new Object[] { slug });
		var c = persistence.crud(Comment.class);
		return Map.of("comments", c.read(c.filter("article", new Object[] { a }), 1));
	}

	@Handle(method = "GET", path = "feed")
	public Object readFeed(Direction direction, Long skip, Long limit, Integer depth, UserHttpExchange<?> exchange) {
		var d = direction != null ? direction : defaultDirection;
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var p = depth != null ? depth : defaultDepth;

		var u = ((PersistenceImpl) persistence).userCrud().filter("followList",
				new Object[] { exchange.sessionUser().id() });
		var ii = !u.isEmpty() ? crud().filterAndCount("author", u.toArray(), d, k, l) : ListPortion.<Long>empty();
		return ii.map(x -> crud().read(x, p));
	}

	@Handle(method = "DELETE", path = "([^/]+)/favorite")
	public Object unfavorite(String slug, UserHttpExchange<?> exchange) {
		var u = exchange.sessionUser();
		if (u == null)
			throw new NullPointerException("user=" + u);
		var c = (ArticleCrud) crud();
		var a = c.read(c.find("slug", new Object[] { slug }));
		c.unfavorite(a.id(), a.createdAt(), (Long) u.id());
		return Map.of("article", a);
	}

	@Handle(method = "PUT", path = "([^/]+)")
	public Object update(String slug, Form form, UserHttpExchange<?> exchange) {
		var s = Objects.requireNonNullElse(toSlug(form.article.title), slug);
		validate(slug, s, form.article);

		var a = crud().update(crud().find("slug", new Object[] { slug }), x -> copier.copy(form.article, x));

		return Map.of("article", a);
	}

	protected String toSlug(String title) {
		return title != null ? NON_ALPHANUMERIC.splitAsStream(title.toLowerCase()).collect(Collectors.joining("-"))
				: null;
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
}
