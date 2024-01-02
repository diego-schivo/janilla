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
class Rendering {

	stack = [];

	get object() {
		return this.stack.at(-1).object;
	}

	get key() {
		return this.stack.at(-1).key;
	}

	get path() {
		return this.stack ? this.stack.map(e => e.key).join('/') : '';
	}

	render = async (object, template, stack) => {
		const s = this.stack;
		if (stack)
			this.stack = stack;
		try {
			return template ? await template(this.renderer(object)) : await object.render();
		} finally {
			this.stack = s;
		}
	}

	renderer = object => {
		return (async (expression, escape) => {
			let v;
			if (expression === '') v = object;
			else {
				const e = typeof expression === 'string' && expression.includes('.') ? expression.split('.') : [expression];
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
			if (escape && typeof v === 'string') v = v.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#039;');
			return v;
		});
	}

	async evaluate(loop) {
		const k = this.key;
		let v;
		for (let i = this.stack.length - 1; i >= 0; i--) {
			const o = this.stack[i].object;
			if (o && Reflect.has(o, 'render') && typeof o.render === 'function') {
				v = await o.render(k);
				if (v !== undefined) break;
			}
			if (!loop) break;
		}
		if (v === undefined)
			for (let i = this.stack.length - 1; i >= 0; i--) {
				const o = this.stack[i].object;
				if (o && Reflect.has(o, k)) {
					v = o[k];
					break;
				}
				if (!loop) break;
			}
		if (Array.isArray(v)) {
			const w = [];
			for (let i = 0; i < v.length; i++) w.push(await this.renderer(v)(i));
			return w.join('');
		}
		if (v && typeof v === 'object' && Reflect.has(v, 'render') && typeof v.render === 'function')
			v = await v.render();
		if (v === undefined || v === null) return '';
		return v;
	}
}

export default Rendering;
