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
export class SlottableElement extends HTMLElement {

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("SlottableElement.connectedCallback");

		this.requestUpdate();
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log("SlottableElement.attributeChangedCallback", "name", name, "oldValue", oldValue, "newValue", newValue);

		if (newValue !== oldValue)
			this.requestUpdate();
	}

	requestUpdate() {
		// console.log("SlottableElement.requestUpdate");

		if (typeof this.updateTimeout === "number")
			clearTimeout(this.updateTimeout);

		this.updateTimeout = setTimeout(async () => {
			this.updateTimeout = undefined;
			await this.update();
		}, 1);
	}

	async update() {
		// console.log("SlottableElement.update");

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
