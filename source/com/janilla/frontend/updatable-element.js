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
export class UpdatableElement extends HTMLElement {

	#state = {};

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("UpdatableElement.connectedCallback");
		this.requestUpdate();
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log("UpdatableElement.attributeChangedCallback", "name", name, "oldValue", oldValue, "newValue", newValue);
		if (newValue !== oldValue && this.isConnected)
			this.requestUpdate();
	}

	requestUpdate() {
		// console.log("UpdatableElement.requestUpdate");
		if (this.#state.ongoing) {
			this.#state.repeat = true;
			return;
		}

		if (typeof this.#state.timeoutID === "number")
			clearTimeout(this.#state.timeoutID);

		this.#state.timeoutID = setTimeout(async () => {
			this.#state.timeoutID = undefined;
			this.#state.ongoing = true;
			await this.update();
			this.#state.ongoing = false;
			if (this.#state.repeat) {
				this.#state.repeat = false;
				this.requestUpdate();
			}
		}, 1);
	}

	async update() {
		// console.log("UpdatableElement.update");
	}
}
