/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
export class UpdatableHTMLElement extends HTMLElement {

	janillas = {
		update: {}
	};

	constructor() {
		super();
		const tn = this.constructor.templateName;
		if (tn)
			this.janillas.initialize = getDocumentFragment(tn).then(x => {
				const df = x.cloneNode(true);
				const tt = [...df.querySelectorAll("template")].map(y => {
					y.remove();
					return y;
				});
				this.janillas.templates = Object.fromEntries([
					["", compileNode(df)],
					...tt.map(y => [y.id, compileNode(y.content)])
				].map(([k, v]) => [k, {
					factory: v,
					functions: []
				}]));
			});
	}

	get state() {
		return this.janillas.state;
	}

	set state(x) {
		this.janillas.state = x;
	}

	connectedCallback() {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).connectedCallback`);
		this.state = {};
		this.requestUpdate();
	}

	disconnectedCallback() {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).disconnectedCallback`);
		this.state = null;
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).attributeChangedCallback`, "name", name, "oldValue", oldValue, "newValue", newValue);
		if (this.isConnected && newValue !== oldValue)
			this.requestUpdate();
	}

	requestUpdate() {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).requestUpdate`);
		const u = this.janillas.update;
		if (u.ongoing) {
			u.repeat = true;
			return;
		}

		if (typeof u.timeoutID === "number")
			clearTimeout(u.timeoutID);

		u.timeoutID = setTimeout(async () => await this.updateTimeout(), 1);
	}

	async updateTimeout() {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).updateTimeout`);
		const i = this.janillas.initialize;
		if (i)
			await i;
		const u = this.janillas.update;
		u.timeoutID = undefined;
		if (!this.isConnected)
			return;
		u.ongoing = true;
		try {
			await this.updateDisplay();
		} finally {
			u.ongoing = false;
		}
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).updateTimeout`, "u.repeat", u.repeat);
		if (u.repeat) {
			u.repeat = false;
			this.requestUpdate();
		}
	}

	async updateDisplay() {
		// console.log(`UpdatableHTMLElement(${this.constructor.name}).updateDisplay`);
		if (this.janillas.templates)
			this.appendChild(this.interpolateDom({
				$template: "",
				...this.state
			}));
	}

	interpolateDom(input) {
		// console.log("FlexibleElement(${this.constructor.name}).interpolateDom");
		const getInterpolate = (template, index) => {
			const x = this.janillas.templates[template];
			if (!x)
				throw new Error(`Template "${template}" not found (${this.constructor.name})`);
			for (let i = x.functions.length; i <= index; i++)
				x.functions.push(x.factory());
			return x.functions[index];
		};
		const indexes = {};
		const interpolate = x => {
			if (x === null || typeof x !== "object")
				return x;
			if (Array.isArray(x))
				return x.map(interpolate);
			if (!Object.hasOwn(x, "$template"))
				return x;
			const y = Object.fromEntries(Object.entries(x).filter(([k, _]) => k !== "$template").map(([k, v]) => [k, interpolate(v)]));
			var k = x.$template;
			indexes[k] ??= 0;
			const i = getInterpolate(k, indexes[k]++);
			return i(y);
		};
		return interpolate(input);
	}
}

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

const expressionRegex = /\$\{(.*?)\}/g;

const compileNode = node => {
	const ii = [];
	const ff = [];
	for (let x = node; x;) {
		if (x instanceof Text) {
			const nv = x.nodeValue;
			if (nv.includes("${") && nv.includes("}")) {
				const ii2 = [...ii];
				x.nodeValue = "";
				const ta = x.parentElement instanceof HTMLTextAreaElement;
				ff.push(n => {
					const n2 = findNode(n, ii2);
					return y => {
						const z = nv.replace(expressionRegex, (_, ex) => evaluate(ex, y) ?? "");
						if (ta) {
							const pe = n2.parentElement;
							if (z !== pe.value)
								pe.value = z;
						} else if (z !== n2.nodeValue)
							n2.nodeValue = z;
					};
				});
			}
		} else if (x instanceof Comment) {
			if (x.nodeValue.startsWith("${") && x.nodeValue.indexOf("}") === x.nodeValue.length - 1) {
				const ii2 = [...ii];
				const ex = x.nodeValue.substring(2, x.nodeValue.length - 1);
				x.nodeValue = "";
				ff.push(n => {
					const n2 = findNode(n, ii2);
					return y => {
						const j2 = (n2.janillas ??= {});
						let rn = n2.nextSibling;
						const nn1 = [];
						for (let i = j2.insertedNodesLength; i > 0; i--) {
							nn1.push(rn);
							rn = rn.nextSibling;
						}
						const z = evaluate(ex, y);
						const zz = Array.isArray(z) ? z : z != null ? [z] : [];
						const nn2 = zz.flatMap(n3 => {
							if (n3 instanceof DocumentFragment) {
								const j3 = (n3.janillas ??= {});
								j3.originalChildNodes ??= [...n3.childNodes];
								if (!n3.firstChild && j3.originalChildNodes.length)
									for (let ps = j3.originalChildNodes[0].previousSibling; ps; ps = ps.previousSibling)
										if (ps instanceof Comment) {
											ps.janillas.insertedNodesLength -= j3.originalChildNodes.length;
											break;
										}
								return j3.originalChildNodes;
							}
							return n3;
						});
						for (const n3 of nn1)
							if (!nn2.includes(n3))
								n2.parentNode.removeChild(n3);
						for (const n3 of nn2.reverse()) {
							if (rn.previousSibling !== n3)
								n2.parentNode.insertBefore(n3, rn);
							rn = n3;
						}
						j2.insertedNodesLength = nn2.length;
					};
				});
			}
		} else if (x instanceof Element && x.hasAttributes()) {
			let i = 0;
			for (const a of x.attributes) {
				const v = a.value;
				if (v.includes("${") && v.includes("}")) {
					const ii2 = [...ii, i];
					a.value = "";
					const s = v.startsWith("${") && v.indexOf("}") === v.length - 1;
					ff.push(n => {
						const a2 = findNode(n, ii2, true);
						const oe = a2.ownerElement;
						return y => {
							let z;
							const v2 = v.replace(expressionRegex, (_, ex) => {
								z = evaluate(ex, y);
								return z ?? "";
							});
							// console.log("v", v, "v2", v2, "z", z, "s", s);
							if (!s)
								;
							else if (z == null || z === false)
								a2.ownerElement?.removeAttributeNode(a2);
							else if (!a2.ownerElement)
								oe.setAttributeNode(a2);
							if (v2 === a2.value)
								return;
							a2.value = z === true ? "" : v2;
							if (a2.name === "value"
								&& (oe instanceof HTMLInputElement || oe instanceof HTMLSelectElement || oe instanceof HTMLTextAreaElement)
								&& a2.value !== oe.value)
								oe.value = a2.value;
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
		if (n instanceof DocumentFragment)
			(n.janillas = {}).originalChildNodes = [...n.childNodes];
		const ff2 = ff.map(x => x(n));
		return x => {
			ff2.forEach(y => y(x));
			return n;
		};
	};
};
