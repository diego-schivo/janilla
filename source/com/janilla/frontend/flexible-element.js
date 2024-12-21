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
import { UpdatableElement } from "./updatable-element.js";

const documentFragments = {};

const getDocumentFragment = async name => {
	const r = `/${name}.html`;
	documentFragments[name] ??= fetch(r).then(x => {
		if (!x.ok)
			throw new Error(`Failed to fetch ${r}: ${x.status} ${x.statusText}`);
		return x.text();
	}).then(x => {
		const t = document.createElement("template");
		t.innerHTML = x;
		return t.content;
	});
	return await documentFragments[name];
};

const evaluate = (expression, context) => {
	let o = context;
	if (expression)
		for (const k of expression.split(".")) {
			if (o == null)
				break;
			const i = k.endsWith("]") ? k.indexOf("[") : -1;
			o = i === -1 ? o[k] : o[k.substring(0, i)]?.[parseInt(k.substring(i + 1, k.length - 1))];
		}
	return o;
};

const findNode = (node, indexes, attribute) => {
	return indexes.reduce((n, x, i) => (attribute && i === indexes.length - 1 ? n.attributes : n.childNodes)[x], node);
}

const compileNode = node => {
	const ii = [];
	const ff = [];
	for (let x = node; x;) {
		if (x instanceof Text) {
			if (x.nodeValue.startsWith("${") && x.nodeValue.endsWith("}")) {
				const ii2 = [...ii];
				const ex = x.nodeValue.substring(2, x.nodeValue.length - 1);
				x.nodeValue = "";
				ff.push(n => {
					const n2 = findNode(n, ii2);
					return y => {
						const z = evaluate(ex, y) ?? "";
						if (z !== n2.nodeValue)
							n2.nodeValue = z;
					};
				});
			}
		} else if (x instanceof Comment) {
			if (x.nodeValue.startsWith("${") && x.nodeValue.endsWith("}")) {
				const ii2 = [...ii];
				const ex = x.nodeValue.substring(2, x.nodeValue.length - 1);
				x.nodeValue = "";
				ff.push(n => {
					const n2 = findNode(n, ii2);
					const m = new Map();
					return y => {
						for (const e of m.entries())
							if (e[0] instanceof DocumentFragment)
								e[0].append(...e[1]);
							else e[1].forEach(z => z.remove());
						m.clear();
						const z = evaluate(ex, y);
						if (!z)
							return;
						const ns = n2.nextSibling;
						(Array.isArray(z) ? z : [z]).forEach(n3 => {
							if (typeof n3 === "string")
								n3 = new Text(n3);
							const ps = ns ? ns.previousSibling : n2;
							n2.parentNode.insertBefore(n3, ns);
							const nn = [];
							for (let ns2 = ps.nextSibling; ns2 !== ns; ns2 = ns2.nextSibling)
								nn.push(ns2);
							m.set(n3, nn);
						});
					};
				});
			}
		} else if (x instanceof Element && x.hasAttributes()) {
			let i = 0;
			for (const a of x.attributes) {
				if (a.value.startsWith("${") && a.value.endsWith("}")) {
					const ii2 = [...ii, i];
					const ex = a.value.substring(2, a.value.length - 1);
					a.value = "";
					ff.push(n => {
						const n2 = findNode(n, ii2, true);
						return y => {
							const z = evaluate(ex, y) ?? "";
							if (z !== n2.value)
								n2.value = z;
						};
					});
				}
				i++;
			}
		}

		if (x.firstChild) {
			x = x.firstChild;
			ii.push(0);
		} else
			do
				if (x === node)
					x = null;
				else if (x.nextSibling) {
					x = x.nextSibling;
					ii[ii.length - 1]++;
					break;
				} else {
					x = x.parentNode;
					ii.pop();
				}
			while (x);
	}
	return () => {
		const n = node.cloneNode(true);
		const ff2 = ff.map(x => x(n));
		return x => {
			ff2.forEach(y => y(x));
			return n;
		};
	};
};

export class FlexibleElement extends UpdatableElement {

	#initialize;

	#domInterpolation;

	constructor() {
		super();
		this.#initialize = getDocumentFragment(this.constructor.templateName).then(x => {
			const df = x.cloneNode(true);
			const tt = [...df.querySelectorAll("template")].map(y => {
				y.remove();
				return y;
			});
			this.#domInterpolation = Object.fromEntries([
				["", compileNode(df)],
				...tt.map((y, i) => [y.id, compileNode(y.content)])
			]);
		});
	}

	async updateDisplay() {
		// console.log("FlexibleElement.updateDisplay");
		await this.#initialize;
	}

	createInterpolateDom(key) {
		return this.#domInterpolation[key ?? ""]();
	}
}
