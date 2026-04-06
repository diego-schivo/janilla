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

export default class PopularTags extends WebComponent {

	static get templateNames() {
		return ["popular-tags"];
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		const f = this.interpolateDom(!hs.tags
			? {
				$template: "",
				loadingSlot: "content"
			}
			: !hs.tags.length
				? {
					$template: "",
					emptySlot: "content"
				}
				: {
					$template: "",
					slot: "content",
					tags: hs.tags.map(x => ({
						$template: "tag",
						text: x
					}))
				});
		this.shadowRoot.append(...f.querySelectorAll("div:has(slot)"));
		this.appendChild(f);

		if (!hs.tags) {
			const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
			const { tags } = await (await fetch(`${apiUrl}/tags`, { headers: apiHeaders })).json();
			history.replaceState({
				...history.state,
				tags
			}, "");
			this.requestDisplay(0);
		}
	}

	handleClick = event => {
		const el = event.target.closest(".tag-default");
		if (el) {
			event.preventDefault();
			event.stopPropagation();
			this.dispatchEvent(new CustomEvent("select-tag", {
				bubbles: true,
				detail: { tag: el.textContent.trim() }
			}));
		}
	}
}
