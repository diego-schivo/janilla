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
/*
const htmlEscapes = {
	"&": "&amp;",
	"<": "&lt;",
	">": "&gt;",
	'"': "&quot;",
	"'": "&#x27;",
	"`": "&#x60;",
};
const unescapedHtml = /[&<>"'`]/g;
const hasUnescapedHtml = new RegExp(unescapedHtml.source);
const escapeHtml = s => s && hasUnescapedHtml.test(s) ? s.replaceAll(unescapedHtml, c => htmlEscapes[c]) : s;
*/

function initDataset(el) {
	let r = el.dataset.render;
	if (!r) {
		r = el.getAttributeNames().some(x => el.getAttribute(x).includes("${"));
		r = el.dataset.render = "";
	}
	el.childNodes.forEach(x => {
		if (x instanceof Element)
			initDataset(x);
		else if (!r && x instanceof Text && x.nodeValue.includes("${"))
			r = el.dataset.render = "";
	});
}

class RenderEngine {

	templates;

	stack = [];

	constructor(templates) {
		if (templates instanceof NodeList)
			templates = Array.from(templates);
		if (Array.isArray(templates)) {
			templates.forEach(x => {
				for (const y of x.content.children)
					initDataset(y);
			});
			templates = Object.fromEntries(templates.map(x => [x.id, x]));
		}
		this.templates = templates;
	}

	render(input) {
		if (input === undefined)
			input = this.stack.pop();
		const s = this.stack.length;
		this.stack.push(input);
		try {
			let v = input.value;
			for (let i = this.stack.length - 1; i >= 0; i--) {
				const y = this.stack[i].value;
				if (typeof y === "object" && y !== null && !Array.isArray(y))
					if (Reflect.has(y, "tryRender") && typeof y.tryRender === "function")
						if (y.tryRender(this))
							break;
			}
			for (; ;) {
				let y = this.stack.at(-1).value;
				if (y === v)
					break;
				if (typeof y === "object" && y !== null && !Array.isArray(y))
					if (Reflect.has(y, "tryRender") && typeof y.tryRender === "function")
						y.tryRender(this);
				v = y;
			}
			const z = this.stack.at(-1);
			if (z.template) {
				const t = this.templates[z.template];
				const df = t.content.cloneNode(true);
				for (const el of df.querySelectorAll("[data-render]")) {
					try {
						const ex = el.dataset.render;
						delete el.dataset.render;
						this.evaluate(ex);
						if (ex.length) {
							const w = this.stack.at(-1).value;
							const a = this.render();
							if (a[0] instanceof Node) {
								el.replaceWith(...a);
								if (typeof w === "object" && w !== null && !Array.isArray(w) && Reflect.has(w, "tryRender") && typeof w.tryRender === "function")
									w.renderedNodes = a;
							} else
								el.outerHTML = a.join("");
						} else {
							for (const n of el.getAttributeNames()) {
								const w = el.getAttribute(n);
								if (w.includes("${"))
									el.setAttribute(n, w.replace(/\$\{(.*?)\}/g, (_, x) => {
										try {
											this.evaluate(x);
											return !x.length ? this.stack.at(-1).value : this.render().join("");
										} finally {
											while (this.stack.length > s + 1)
												this.stack.pop();
										}
									}));
							}
							for (const n of el.childNodes)
								if (n instanceof Text && n.nodeValue.includes("${"))
									n.nodeValue = n.nodeValue.replace(/\$\{(.*?)\}/g, (_, x) => {
										try {
											this.evaluate(x);
											return !x.length ? this.stack.at(-1).value : this.render().join("");
										} finally {
											while (this.stack.length > s + 1)
												this.stack.pop();
										}
									});
						}
					} finally {
						while (this.stack.length > s + 1)
							this.stack.pop();
					}
				}
				return Array.from(df.children);
			}
			if (Array.isArray(z.value)) {
				const a = [];
				for (let i = 0; i < z.value.length; i++)
					a.push(... this.render({ key: i, value: z.value[i] }));
				return a;
			}
			return z.value != null ? [z.value] : [];
		} finally {
			while (this.stack.length > s)
				this.stack.pop();
		}
	}

	match(pattern, callback) {
		const i = this.stack.length - pattern.length;
		if (i < 0)
			return false;
		let j = i;
		for (const x of pattern) {
			if (x !== undefined) {
				const y = this.stack[j];
				if (x === y.value);
				else if (typeof x === "string" && x.startsWith("[") && x.endsWith('"]')) {
					const i = x.indexOf('="');
					if (i === -1)
						return false;
					switch (x.substring(1, i)) {
						case "key":
							if (x.substring(i + 2, x.length - 2) !== y.key)
								return false;
							break;
						case "type":
							if (x.substring(i + 2, x.length - 2) !== typeof y.key)
								return false;
							break;
						default:
							return false;
					}
				} else
					return false;
			}
			j++;
		}
		callback(this.stack.slice(i).map(x => x.value), this.stack.at(-1));
		return true;
	}

	clone() {
		const e = new RenderEngine(this.templates);
		e.stack = [...this.stack];
		return e;
	}

	evaluate(expression) {
		if (expression.length === 0)
			return;
		let c = this.stack.at(-1);
		const nn = expression.split(".");
		for (let i = 0; i < nn.length; i++) {
			let n = nn[i];
			let k;
			if (n.endsWith("]")) {
				const j = n.lastIndexOf("[");
				k = parseInt(n.substring(j + 1, n.length - 1), 10);
				n = n.substring(0, j);
			} else
				k = -1;
			let d = { key: n };
			for (let i = this.stack.length - 1; i >= 0; i--) {
				c = this.stack[i];
				if (c.value instanceof FormData) {
					d.value = c.value.get(n);
					break;
				} else if (typeof c.value === "object" && c.value !== null) {
					if (n.length) {
						if (Reflect.has(c.value, n)) {
							d.value = c.value[n];
							break;
						}
					} else if (k >= 0) {
						d.key = k;
						d.value = c.value[k];
						break;
					}
				}
			}
			this.stack.push(d);
			if (n.length && k >= 0) {
				d = { key: k, value: d.value[k] };
				stack.push(d);
			}
			if (d.value == null)
				break;
		}
	}
}

export default RenderEngine;
