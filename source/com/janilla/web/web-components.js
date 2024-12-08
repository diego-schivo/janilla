/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
import { compileNode } from "./dom.js";

export class UpdatableElement extends HTMLElement {

	#update = {};

	constructor() {
		super();

		this.interpolatorBuilders ??= loadTemplate(this.constructor.templateName).then(t => {
			const c = t.content.cloneNode(true);
			const cc = [...c.querySelectorAll("template")].map(x => {
				x.remove();
				return x.content;
			});
			return [compileNode(c), ...cc.map(x => compileNode(x))];
		});
	}

	connectedCallback() {
		// console.log("UpdatableElement.connectedCallback");
		this.requestUpdate();
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log("UpdatableElement.attributeChangedCallback", "name", name, "oldValue", oldValue, "newValue", newValue);
		if (newValue !== oldValue)
			this.requestUpdate();
	}

	requestUpdate() {
		// console.log("UpdatableElement.requestUpdate");
		if (this.#update.ongoing) {
			this.#update.repeat = true;
			return;
		}

		if (typeof this.#update.timeoutID === "number")
			clearTimeout(this.#update.timeoutID);

		this.#update.timeoutID = setTimeout(async () => {
			this.#update.timeoutID = undefined;
			this.#update.ongoing = true;
			await this.update();
			this.#update.ongoing = false;
			if (this.#update.repeat) {
				this.#update.repeat = false;
				this.requestUpdate();
			}
		}, 1);
	}

	async update() {
		// console.log("UpdatableElement.update");
	}
}

export class SlottableElement extends UpdatableElement {

	constructor() {
		super();
	}

	async update() {
		// console.log("SlottableElement.update");
		await super.update();

		if (!this.slot)
			this.state = undefined;
		await this.render();
		if (!this.slot || this.state)
			return;

		await this.computeState();
		if (this.state)
			await this.render();
	}

	async computeState() {
		// console.log("SlottableElement.computeState");
	}

	async render() {
		// console.log("SlottableElement.render");
	}
}

const templates = {};

export async function loadTemplate(name) {
	templates[name] ??= fetch(`/${name}.html`).then(x => x.text()).then(x => {
		const t = document.createElement("template");
		t.innerHTML = x;
		return t;
	});
	return await templates[name];
}
