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

	get target() {
		for (let i = this.stack.length - 1; i >= 0; i--) {
			const t = this.stack[i].target;
			if (t != null)
				return t;
		}
	}

	get key() {
		if (this.stack.length)
			return this.stack.at(-1).key;
	}

	async render(target, template) {
		const t = typeof template === 'string' ? this.templates[template] : template;
		const r = this.renderer(target);
		return (t ? await t(r) : await r()) ?? '';
	}

	renderer(target) {
		const r = async (expression, escape) => {
			let v;
			if (expression === '')
				v = target;
			else {
				const e = typeof expression === 'string' ? expression.split('.') : [expression];
				let t = target, i = 0;
				try {
					for (const k of e) {
						this.stack.push(t !== this.target ? { target: t, key: k } : { key: k });
						v = await this.evaluate(i == 0);
						t = v;
						i++;
					}
				} finally {
					for (; i > 0; i--)
						this.stack.pop();
				}
			}
			if (escape && typeof v === 'string')
				v = escapeHtml(v);
			return v;
		};
		return r;
	}

	async evaluate(loop) {
		let v;
		for (let i = this.stack.length - 1; i >= 0; i--) {
			const t = this.stack[i].target;
			if (typeof t === 'object' && t !== null && !Array.isArray(t)) {
				const o = t;
				if (Reflect.has(o, 'render') && typeof o.render === 'function') {
					v = await o.render(this);
					if (v !== undefined)
						break;
				}
			}
		}
		if (v === undefined) {
			const k = this.key;
			if (k != null)
				for (let i = this.stack.length - 1; i >= 0; i--) {
					const t = this.stack[i].target;
					if (t == null)
						continue;

					if (t instanceof FormData) {
						v = t.get(k);
						break;
					} else if (typeof t === 'object' && t !== null && Reflect.has(t, k)) {
						v = t[k];
						break;
					} else if (!loop)
						break;
				}
			if (v === undefined && k === undefined) {
				v = this.target;
				if (Array.isArray(v)) {
					const r = this.renderer(v);
					const a = [];
					for (let i = 0; i < v.length; i++)
						a.push(await r(i));
					return a.join('');
				}
				return v;
			}
		}
		if (v !== this.target && v != null) {
			const r = this.renderer(v);
			return await r();
		}
		return v ?? '';
	}

	clone() {
		const e = new RenderEngine();
		e.stack = [...this.stack];
		return e;
	}

	isRendering(target, key, indexed) {
		if (target && !key && !indexed) {
			if (!this.stack.length)
				return false;
			const p = this.stack.at(-1);
			return target === p.target && p.key === undefined;
		}

		if (key) {
			if (indexed)
				return this.stack.length >= 4
					&& key === this.stack.at(-4).key
					&& Array.isArray(this.stack.at(-3).target)
					&& typeof this.stack.at(-2).key === 'number'
					&& this.stack.at(-1).key === undefined;
			return key === this.key && (!target || target === this.target);
		}

		return false;
	}
}

export default RenderEngine;
