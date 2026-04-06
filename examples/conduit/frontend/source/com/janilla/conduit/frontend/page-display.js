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

export default class PageDisplay extends WebComponent {

	static get observedAttributes() {
		return ["data-loading", "data-path"];
	}

	static get templateNames() {
		return ["page-display"];
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	async updateDisplay() {
		const nn = this.dataset.path.split("/");
		//console.log("nn", JSON.stringify(nn));
		const hs = history.state ?? {};
		const o = {
			$template: "",
			article: (() => {
				const x = nn[1] === "article";
				return {
					$template: "article",
					slot: x ? (() => {
						/*
						if (hs.article && hs.article.slug !== nn[2]) {
							delete hs.article;
							history.replaceState(hs, "");
						}
						*/
						return hs.article ? "content" : "content2";
					})() : null,
					slug: x ? nn[2] : null
				};
			})(),
			editor: (() => {
				const x = nn[1] === "editor";
				return {
					$template: "editor",
					slot: x ? (() => {
						/*
						if (hs.article && hs.article.slug !== nn[2]) {
							delete hs.article;
							history.replaceState(hs, "");
						}
						*/
						return hs.article ? "content" : "content2";
					})() : null,
					slug: x ? nn[2] : null
				};
			})(),
			home: {
				$template: "home",
				slot: nn[1] === "" ? "content" : null
			},
			login: {
				$template: "login",
				slot: nn[1] === "login" ? "content" : null
			},
			profile: (() => {
				const u = nn[1]?.startsWith("@") ? decodeURIComponent(nn[1].substring(1)) : null;
				return {
					$template: "profile",
					slot: u ? (() => {
						/*
						if (hs.profile && hs.profile.username !== u) {
							delete hs.profile;
							history.replaceState(hs, "");
						}
						*/
						return hs.profile ? "content" : "content2";
					})() : null,
					username: u
				};
			})(),
			register: {
				$template: "register",
				slot: nn[1] === "register" ? "content" : null
			},
			settings: {
				$template: "settings",
				slot: nn[1] === "settings" ? "content" : null
			}
		};

		const ce = Object.entries(o).find(([k, v]) => k !== "$template" && v.slot === "content");
		const s = this.customState;
		if (ce)
			s.contentEntry = ce;
		else if (s.contentEntry && !o[s.contentEntry[0]].slot)
			o[s.contentEntry[0]] = s.contentEntry[1];
		//console.log("o", JSON.stringify(o));

		const f = this.interpolateDom(Object.fromEntries(Object.entries(o).filter(([k, v]) => k === "$template" || v.slot)));
		this.shadowRoot.append(...f.querySelectorAll("link, slot"));
		this.appendChild(f);
	}
}
