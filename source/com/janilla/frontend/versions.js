/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class VersionsView extends UpdatableHTMLElement {

	static get templateName() {
		return "versions";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const hh = ["updatedAt", "id", "status"];
		this.appendChild(this.interpolateDom({
			$template: "",
			heads: hh.map(x => ({
				$template: "head",
				text: x
			})),
			rows: ap.state.versions.map(x => ({
				$template: "row",
				cells: (() => {
					const cc = hh.map(y => ({
						content: (() => {
							let v = x[y];
							if (y === "updatedAt")
								v = ap.dateTimeFormat.format(new Date(v))
							return v;
						})()
					}));
					cc[0].href = `/admin${ap.dataset.path}/${x.id}`;
					return cc;
				})().map(y => ({
					$template: y.href ? "link-cell" : "cell",
					...y
				}))
			}))
		}));
	}
}
