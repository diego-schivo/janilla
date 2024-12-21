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
import { FlexibleElement } from "./flexible-element.js";

export class SlottableElement extends FlexibleElement {

	constructor() {
		super();
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log(`SlottableElement.attributeChangedCallback`, "name", name, "oldValue", oldValue, "newValue", newValue);
		super.attributeChangedCallback(name, oldValue, newValue);
		if (!this.slot)
			this.state = null;
	}

	async updateDisplay() {
		// console.log("SlottableElement.updateDisplay");
		await super.updateDisplay();
		this.renderState();
		if (this.slot && !this.state) {
			const s = await this.computeState();
			if (this.slot) {
				this.state = s;
				this.requestUpdate();
			}
		}
	}

	async computeState() {
		// console.log("SlottableElement.computeState");
	}

	renderState() {
		// console.log("SlottableElement.renderState");
	}
}
