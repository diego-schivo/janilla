/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import WebComponent from "./web-component.js";

export default class CmsArray extends WebComponent {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateNames() {
		return ["cms-array"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
	}

	handleChange = event => {
		const el = event.target;
		const s = this.state;
		//if (el.matches("[name]"))
		if (el.matches("select:not([name])")) {
			event.stopPropagation();
			const li = el.closest("li");
			const i = Array.prototype.indexOf.call(li.parentElement.children, li);
			const swap = (oo, i1, i2) => [oo[i1], oo[i2]] = [oo[i2], oo[i1]];
			switch (el.value) {
				case "move-down":
					swap(s.field.data, i, i + 1);
					swap(s.items, i, i + 1);
					break;
				case "move-up":
					swap(s.field.data, i, i - 1);
					swap(s.items, i, i - 1);
					break;
				case "remove":
					s.field.data.splice(i, 1);
					s.items.splice(i, 1);
					break;
			}
			this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
			this.requestDisplay();
		} else if (s.dialog && el.matches('[type="radio"]')) {
			event.stopPropagation();
			delete s.dialog;
			const a = this.closest("cms-admin");
			a.initField(s.field);
			s.field.data.push({ $type: el.value });
			//console.log("x", s.field.data);
			s.items.push({
				key: s.nextKey++,
				expand: true
			});
			this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
			this.requestDisplay();
		} else if (el.matches('[type="checkbox"]:not([name])')) {
			event.stopPropagation();
			const li = el.closest("li");
			const i = Array.prototype.indexOf.call(li.parentElement.children, li);
			s.items[i].expand = el.checked;
		}
	}

	handleClick = event => {
		const el = event.target.closest("button");
		if (el) {
			event.stopPropagation();
			const s = this.state;
			switch (el.name) {
				case "add":
					if (s.field.elementTypes.length === 1) {
						const a = this.closest("cms-admin");
						a.initField(s.field);
						s.field.data.push({ $type: s.field.elementTypes[0] });
						s.items.push({
							key: s.nextKey++,
							expand: true
						});
						this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
					} else
						s.dialog = true;
					break;
				case "close":
					delete s.dialog;
					break;
				case "collapse-all":
					s.items.forEach(x => x.expand = false);
					break;
				case "show-all":
					s.items.forEach(x => x.expand = true);
					break;
			}
			this.requestDisplay();
		}
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= (() => {
			const a = this.closest("cms-admin");
			return a.field(p);
		})();
		s.nextKey ??= s.field.data?.length ?? 0;
		s.items ??= Array.from({ length: s.nextKey }, (_, i) => ({
			key: i,
			expand: false
		}));
		this.appendChild(this.interpolateDom({
			$template: "",
			label: this.dataset.label,
			items: s.field.data?.map((x, i, xx) => ({
				$template: "item",
				title: x.$type.split(/(?=[A-Z])/).join(" "),
				options: (() => {
					const oo = [];
					oo.push({ text: "\u2026" });
					if (i > 0)
						oo.push({
							value: "move-up",
							text: "Move Up"
						});
					if (i < xx.length - 1)
						oo.push({
							value: "move-down",
							text: "Move Down"
						});
					oo.push({
						value: "remove",
						text: "Remove"
					});
					return oo;
				})().map((y, j) => ({
					$template: "option",
					...y,
					selected: j === 0
				})),
				checked: s.items[i].expand,
				field: {
					$template: "object",
					path: `${p}.${i}`,
					updatedAt: this.dataset.updatedAt,
					arrayKey: s.items[i].key
				}
			})),
			dialog: s.dialog ? {
				$template: "dialog",
				types: s.field.elementTypes.map(x => ({
					$template: "type",
					label: x.split(/(?=[A-Z])/).join(" "),
					value: x,
					checked: false
				}))
			} : null
		}));
	}
}
