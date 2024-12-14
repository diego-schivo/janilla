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
const evaluate = (expression, context) => {
	let o = context;
	for (const k of expression.split("."))
		if (o != null && k)
			o = o[k];
		else
			break;
	return o;
};

const findNode = (node, indexes, attribute) => {
	return indexes.reduce((n, x, i) => (attribute && i === indexes.length - 1 ? n.attributes : n.childNodes)[x], node);
}

export const compileNode = node => {
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

export const removeAllChildren = element => {
	while (element.firstChild)
		element.removeChild(element.lastChild);
};

export const matchNode = (xpath, context, not) => {
	const q = resolve => {
		const n = context.ownerDocument.evaluate(xpath, context, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue
		if (not ? !n : n) {
			console.log("matchNode", xpath, n);
			resolve(n);
		}
		return n;
	};
	let o, t;
	const r = resolve => {
		return element => {
			if (o) {
				clearTimeout(t);
				o.disconnect();
				o = null;
			}
			setTimeout(() => resolve(element), 50);
		};
	};
	return new Promise((resolve, reject) => {
		const n = q(r(resolve));
		if (not ? n : !n) {
			o = new MutationObserver(() => q(r(resolve)));
			o.observe(context, { childList: true, subtree: true });
			t = setTimeout(() => {
				if (o) {
					o.disconnect();
					o = null;
				}
				reject(`Timeout (xpath=${xpath})`);
			}, 500);
		} else if (not)
			reject(`Not found (xpath=${xpath})`);
	});
};
