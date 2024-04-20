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
import templates from './templates.js';

const htmlEscapes = {
	"&": "&amp",
	"<": "&lt",
	">": "&gt",
	'"': "&quot",
	"'": "&#x27",
	"`": "&#x60",
};
const unescapedHtml = /[&<>"'`]/g;
const hasUnescapedHtml = new RegExp(unescapedHtml.source);
const escapeHtml = s => s && hasUnescapedHtml.test(s) ? s.replaceAll(unescapedHtml, c => htmlEscapes[c]) : s;

class RenderEngine {

	templates = templates;

	stack = [];

	async render(input) {
		if (input === undefined)
			input = this.stack.pop();
		const s = this.stack.length;
		this.stack.push(input);
		try {
			let v = input.value;
			for (let i = this.stack.length - 1; i >= 0; i--) {
				const y = this.stack[i].value;
				if (typeof y === 'object' && y !== null && !Array.isArray(y))
					if (Reflect.has(y, 'render') && typeof y.render === 'function')
						if (await y.render(this))
							break;
			}
			for (; ;) {
				let y = this.stack.at(-1).value;
				if (y === v)
					break;
				if (typeof y === 'object' && y !== null && !Array.isArray(y))
					if (Reflect.has(y, 'render') && typeof y.render === 'function')
						await y.render(this);
				v = y;
			}
			const z = this.stack.at(-1);
			if (z.template) {
				const t = this.templates[z.template];
				return await t(async (expression, escape) => {
					try {
						this.evaluate(expression);
						if (expression.length === 0)
							return this.stack.at(-1).value;
						const c = this.stack.pop();
						return await this.render(c);
					} finally {
						while (this.stack.length > s + 1)
							this.stack.pop();
					}
				});
			}
			if (Array.isArray(z.value)) {
				const a = [];
				for (let i = 0; i < z.value.length; i++)
					a.push(await this.render({ key: i, value: z.value[i] }));
				return a.join('');
			}
			return z.value ?? '';
		} finally {
			while (this.stack.length > s)
				this.stack.pop();
		}
	}

	async match(pattern, callback) {
		const i = this.stack.length - pattern.length;
		if (i < 0)
			return false;
		let j = i;
		for (const x of pattern) {
			if (x !== undefined) {
				const y = this.stack[j];
				if (x !== y.value && x !== y.key && x !== typeof y.key)
					return false;
			}
			j++;
		}
		await callback(this.stack.slice(i).map(x => x.value), this.stack.at(-1));
		return true;
	}

	clone() {
		const e = new RenderEngine();
		e.stack = [...this.stack];
		return e;
	}

	evaluate(expression) {
		if (expression.length === 0)
			return;
		let c = this.stack.at(-1);
		const nn = expression.split('.');
		for (let i = 0; i < nn.length; i++) {
			let n = nn[i];
			let k;
			if (n.endsWith(']')) {
				const j = n.lastIndexOf('[');
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
				} else if (typeof c.value === 'object' && c.value !== null) {
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
