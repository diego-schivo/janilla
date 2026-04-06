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
import WebComponent from "./web-component.js";
import { formatMarkdownAsHtml, parseMarkdown } from "./markdown.js";

export default class Article extends WebComponent {

	static get observedAttributes() {
		return ["data-slug", "slot"];
	}

	static get templateNames() {
		return ["article"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("add-comment", this.handleAddComment);
		this.addEventListener("click", this.handleClick);
		this.addEventListener("remove-comment", this.handleRemoveComment);
		this.addEventListener("toggle-favorite", this.handleToggleFavorite);
		this.addEventListener("toggle-follow", this.handleToggleFollow);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("add-comment", this.handleAddComment);
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("remove-comment", this.handleRemoveComment);
		this.removeEventListener("toggle-favorite", this.handleToggleFavorite);
		this.removeEventListener("toggle-follow", this.handleToggleFollow);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		if (hs.article) {
			const s = this.customState;
			s.body ??= (() => {
				const x = document.createElement("template");
				x.innerHTML = formatMarkdownAsHtml(parseMarkdown(hs.article.body));
				return x.content;
			})();
			this.appendChild(this.interpolateDom({
				$template: "",
				...hs.article,
				body: s.body,
				comments: hs.comments,
				meta: Array.from({ length: 2 }, () => ({
					$template: "meta",
					...hs.article,
					content: hs.article.author.username === this.closest("app-element").customState.user?.username
						? ({ $template: "can-modify" })
						: ({
							$template: "cannot-modify",
							...hs.article
						})
				})),
				tagItems: hs.article.tagList.map(x => ({
					$template: "tag-item",
					text: x
				}))
			}));
		} else {
			const a = this.closest("app-element");
			const [{ article }, { comments }] = await Promise.all([
				`${a.dataset.apiUrl}/articles/${this.dataset.slug}`,
				`${a.dataset.apiUrl}/articles/${this.dataset.slug}/comments`,
			].map(x => fetch(x, { headers: a.customState.apiHeaders }).then(y => y.json())));
			Object.assign(hs, { article, comments });
			history.replaceState(hs, "");
			dispatchEvent(new CustomEvent("popstate"));
		}
	}

	handleAddComment = event => {
		const hs = history.state;
		hs.comments.unshift(event.detail.comment);
		history.replaceState(hs, "");
		this.querySelector("comments-element").requestDisplay();
	}

	handleClick = async event => {
		const el = event.target.closest(".article-meta a:not([href]), .article-meta button");
		if (el) {
			event.preventDefault();
			if (el.matches("a"))
				location.hash = `#/editor/${this.dataset.slug}`;
			else {
				const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
				const r = await fetch(`${apiUrl}/articles/${this.dataset.slug}`, {
					method: "DELETE",
					headers: apiHeaders
				});
				if (r.ok)
					location.hash = "#/";
			}
		}
	}

	handleRemoveComment = event => {
		const hs = history.state;
		const cc = hs.comments;
		const i = cc.findIndex(x => x === event.detail.comment);
		cc.splice(i, 1);
		history.replaceState(hs, "");
		this.querySelector("comments-element").requestDisplay();
	}

	handleToggleFavorite = event => {
		const { article } = event.detail;
		history.replaceState({
			...history.state,
			article
		}, "");
		this.requestDisplay();
	}

	handleToggleFollow = event => {
		const hs = history.state;
		hs.article.author = event.detail.profile;
		history.replaceState(hs, "");
		this.requestDisplay();
	}
}
