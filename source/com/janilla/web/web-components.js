/*
 * MIT License
 *
 * Copyright (c) 2024 Diego Schivo
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
