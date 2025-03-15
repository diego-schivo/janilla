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

export default class ObjectControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "object-control";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const p = this.dataset.path;
		const f = ap.field(p);
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			...f,
			items: (() => {
				const pp = Object.entries(f.properties).filter(([k, _]) => k !== "id");
				const ff = pp.map(([k, v]) => {
					const ct = ap.controlTemplate(ap.field(k, f));
					const p2 = p ? `${p}.${k}` : k;
					return {
						$template: ["select-control", "text-control", "textarea-control"].includes(ct) ? "label-nest-field"
							: ["checkbox-control"].includes(ct) ? "no-label-field"
								: "label-field",
						label: ap.label(p2),
						name: k,
						control: {
							$template: ct,
							path: p2,
							key: this.dataset.key,
							...v
						}
					};
				});
				const nn = ap.tabs(f.type);
				let ii;
				if (nn) {
					const kk = pp.map(([k, _]) => k);
					ii = Object.fromEntries(Object.values(nn).flatMap(x => x.map(y => [y, kk.indexOf(y)])).filter((_, i) => i !== -1));
				}
				const jj = (nn ? ff.filter(x => !Object.hasOwn(ii, x.name)) : ff).map(x => ({
					$template: "item",
					field: x
				}));
				if (nn)
					jj.splice(Object.values(ii)[0], 0, {
						$template: "item",
						field: {
							$template: "tabs",
							buttons: Object.keys(nn).map((x, i) => ({
								$template: "tabs-button",
								index: i,
								selected: `${i === 0}`,
								label: x
							})),
							panels: Object.values(nn).map((x, i) => ({
								$template: "tabs-panel",
								index: i,
								hidden: i !== 0,
								items: (() => {
									return x.map(y => ({
										$template: "item",
										field: ff.find(z => z.name === y)
									}));
								})()
							}))
						}
					});
				return jj;
			})()
		}));
	}
}
