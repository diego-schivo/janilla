/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
import WebComponent from "base/web-component";

const documents = {};
const parser = new DOMParser();

export default class LucideIcon extends WebComponent {

	static get observedAttributes() {
		return ["data-name"];
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const s = this.customState ?? this.state;
		if (this.dataset.name === s.name)
			return;
		s.name = this.dataset.name;
		while (this.firstChild)
			this.removeChild(this.lastChild);
		if (!s.name)
			return;
		documents[s.name] ??= fetch(`/icons/${s.name}.svg`).then(x => x.text()).then(x => {
			return parser.parseFromString(x, "image/svg+xml");
		});
		const d = await documents[s.name];
		this.appendChild(d.firstChild.cloneNode(true));
	}
}
