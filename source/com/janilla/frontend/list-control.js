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

export default class ListControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "list-control";
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
			s.keys.splice(i, 1);
			this.requestUpdate();
		} else if (event.target.matches('[type="radio"]')) {
			event.stopPropagation();
			event.target.closest("dialog").close();
			s.field.data.push({ $type: event.target.value });
			s.keys.push(s.nextKey++);
			this.requestUpdate();
		}
	}

	handleClick = event => {
		if (!event.target.matches('[type="button"]'))
			return;
		event.stopPropagation();
		this.querySelector(":scope > dialog").showModal();
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= (() => {
			const af = this.closest("admin-panel");
			return af.field(p);
		})();
		s.nextKey ??= s.field.data?.length ?? 0;
		s.keys ??= Array.from({ length: s.nextKey }, (_, i) => i);
		this.appendChild(this.interpolateDom({
			$template: "",
			items: s.field.data?.map((x, i) => ({
				$template: "item",
				type: x.$type,
				field: {
					$template: "object",
					path: `${p}.${i}`,
					key: s.keys[i]
				}
			})),
			types: s.field.elementTypes.map(x => ({
				$template: "type",
				name: x,
				checked: false
			}))
		}));
	}
}
