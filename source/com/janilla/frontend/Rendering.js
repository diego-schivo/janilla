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
const reUnescapedHtml = /[&<>"'`]/g;
const reHasUnescapedHtml = new RegExp(reUnescapedHtml.source);
const escapeHtml = s => s && reHasUnescapedHtml.test(s) ? s.replace(reUnescapedHtml, c => htmlEscapes[c]) : s;

class Rendering {

	templates = templates;

	stack = [];

	get object() {
		return this.stack.at(-1).object;
	}

	get key() {
		return this.stack.at(-1).key;
	}

	async render(object, template) {
		if (typeof template === 'string') template = this.templates[template];
		if (template)
			return await template(this.renderer(object));
		if (Array.isArray(object)) {
			const r = this.renderer(object);
			const a = [];
			for (let i = 0; i < object.length; i++) a.push(await r(i));
			return a.join('');
		}
		if (object && typeof object === 'object' && Reflect.has(object, 'render') && typeof object.render === 'function')
			object = await object.render(undefined, this);
		object ??= '';
		return object;
	}

	renderer(object) {
		return (async (expression, escape) => {
			let v;
			if (expression === '') v = object;
			else {
				const e = typeof expression === 'string' ? expression.split('.') : [expression];
				let o = object, i = 0;
				try {
					for (let k of e) {
						this.stack.push({ object: o, key: k });
						v = await this.evaluate(i == 0);
						o = v;
						i++;
					}
				} finally {
					for (; i > 0; i--)
						this.stack.pop();
				}
			}
			// if (escape && typeof v === 'string') v = v.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#039;');
			if (escape && typeof v === 'string') v = escapeHtml(v);
			return v;
		});
	}

	async evaluate(loop) {
		const k = this.key;
		let v;
		for (let i = this.stack.length - 1; i >= 0; i--) {
			const o = this.stack[i].object;
			if (o && Reflect.has(o, 'render') && typeof o.render === 'function') {
				v = await o.render(k, this);
				if (v !== undefined) break;
			}
		}
		if (v === undefined)
			for (let i = this.stack.length - 1; i >= 0; i--) {
				const o = this.stack[i].object;
				if (!o)
					;
				else if (o instanceof FormData) {
					v = o.get(k);
					break;
				} else if (Reflect.has(o, k)) {
					v = o[k];
					break;
				}
				if (!loop) break;
			}
		return await this.render(v);
	}

	clone() {
		const r = new Rendering();
		r.stack = [...this.stack];
		return r;
	}
}

export default Rendering;
