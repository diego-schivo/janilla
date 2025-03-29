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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class CmsArray extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "cms-array";
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
		const s = this.state;
		if (event.target.matches("select")) {
			event.stopPropagation();
			const li = event.target.closest("li");
			const i = Array.prototype.indexOf.call(li.parentElement.children, li);
			s.field.data.splice(i, 1);
			s.items.splice(i, 1);
			this.requestUpdate();
		} else if (event.target.matches('[type="radio"]')) {
			event.stopPropagation();
			event.target.closest("dialog").close();
			const a = this.closest("cms-admin");
			a.initField(s.field);
			s.field.data.push({ $type: event.target.value });
			console.log("x", s.field.data);
			s.items.push({
				key: s.nextKey++,
				expand: true
			});
			console.log("x", JSON.stringify(a.state.entity));
			this.requestUpdate();
		} else if (event.target.matches('[type="checkbox"]')) {
			event.stopPropagation();
			const li = event.target.closest("li");
			const i = Array.prototype.indexOf.call(li.parentElement.children, li);
			s.items[i].expand = event.target.checked;
		}
	}

	handleClick = event => {
		const b = event.target.closest("button");
		if (b) {
			event.stopPropagation();
			const s = this.state;
			if (s.field.elementTypes.length === 1) {
				const a = this.closest("cms-admin");
				a.initField(s.field);
				s.field.data.push({ $type: s.field.elementTypes[0] });
				s.items.push({
					key: s.nextKey++,
					expand: true
				});
				this.requestUpdate();
			} else
				this.querySelector(":scope > dialog").showModal();
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
			items: s.field.data?.map((x, i) => ({
				$template: "item",
				title: x.$type,
				checked: s.items[i].expand,
				field: {
					$template: "object",
					path: `${p}.${i}`,
					key: s.items[i].key
				}
			})),
			types: s.field.elementTypes.map(x => ({
				$template: "type",
				name: x,
				expand: false
			}))
		}));
	}
}
