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
import tests from "./tests.js";

export default class TestBench extends FlexibleElement {

	static get observedAttributes() {
		return ["data-values"];
	}

	static get templateName() {
		return "test-bench";
	}

	state = Object.entries(tests).map((x, i) => ({
		value: i,
		text: x[0]
	}));

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("TestBench.connectedCallback");
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		// console.log("TestBench.disconnectedCallback");
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleChange = event => {
		// console.log("TestBench.handleChange", event);
		if (!event.target.matches(":not([name])"))
			return;
		this.querySelectorAll('[name="test"]').forEach(x => x.checked = event.target.checked);
	}

	handleLoad = event => {
		// console.log("TestBench.handleLoad", event);
		const k = this.keys.shift();
		const s = this.state[k];
		tests[s.text](event.target.contentDocument).then(async () => {
			await new Promise(x => setTimeout(x, 200));
			s.class = "succeeded";
		}).catch(async error => {
			await new Promise(x => setTimeout(x, 200));
			s.class = "failed";
			s.title = error;
		}).finally(async () => {
			await fetch("/test/stop", { method: "POST" });
			this.interpolateTestFrame = null;
			this.requestUpdate();
			if (!this.keys.length)
				this.querySelectorAll("input, button").forEach(x => x.disabled = false);
		});
	}

	handleSubmit = event => {
		// console.log("TestBench.handleSubmit", event);
		event.preventDefault();
		this.keys = new FormData(event.target).getAll("test");
		this.querySelectorAll("input, button").forEach(x => x.disabled = true);
		this.state.forEach(x => x.class = null);
		this.requestUpdate();
	}

	async updateDisplay() {
		// console.log("TestBench.updateDisplay");
		this.querySelector("iframe")?.removeEventListener("load", this.handleLoad);
		await super.updateDisplay();
		if (this.keys?.length) {
			await fetch("/test/start", { method: "POST" });
			localStorage.removeItem("jwtToken");
			this.state[this.keys[0]].class = "ongoing";
		}
		this.interpolate ??= this.createInterpolateDom();
		this.appendChild(this.interpolate({
			testItems: (() => {
				if (this.interpolateTestItems?.length !== this.state.length)
					this.interpolateTestItems = this.state.map(() => this.createInterpolateDom("test-item"));
				return this.state.map((x, i) => this.interpolateTestItems[i](x));
			})(),
			testFrame: this.keys?.length ? (() => {
				this.interpolateTestFrame ??= this.createInterpolateDom("test-frame");
				return this.interpolateTestFrame();
			})() : null
		}));
		this.querySelector("iframe")?.addEventListener("load", this.handleLoad);
	}
}