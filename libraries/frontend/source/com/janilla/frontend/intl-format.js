/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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

const amountFormatter = new Intl.NumberFormat("en-US", {
	style: "currency",
	currency: "USD"
});

const dateFormatter = new Intl.DateTimeFormat("en-US", {
	day: "numeric",
	month: "short",
	year: "numeric",
});

const formatters = {
	amount: x => amountFormatter.format(x),
	date: x => dateFormatter.format(new Date(x))
};

export default class IntlFormat extends WebComponent {

	static get observedAttributes() {
		return ["data-type", "data-value"];
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		if (!this.dataset.value) {
			this.textContent = "";
			return;
		}
		const f = this.dataset.type ? formatters[this.dataset.type] : null;
		this.textContent = f ? f(this.dataset.value) : this.dataset.value;
	}
}
