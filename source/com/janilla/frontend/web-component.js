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
export class WebComponent extends HTMLElement {

	#initializeTemplating;

	#templating;

	#displayUpdate = {};

	constructor() {
		super();
		const tn = this.constructor.templateName;
		if (!tn)
			return;
		this.#initializeTemplating = getDocumentFragment(tn).then(x => {
			const df = x.cloneNode(true);
			const tt = [...df.querySelectorAll("template")].map(y => {
				y.remove();
				return y;
			});
			this.#templating = Object.fromEntries([
				["", compileNode(df)],
				...tt.map(y => [y.id, compileNode(y.content)])
			].map(([k, v]) => [k, {
				factory: v,
				functions: []
			}]));
		});
	}

	connectedCallback() {
		// console.log(`WebComponent(${this.constructor.name}).connectedCallback`);
		this.state = {};
		this.requestDisplay();
	}

	disconnectedCallback() {
		// console.log(`WebComponent(${this.constructor.name}).disconnectedCallback`);
		delete this.state;
	}

	attributeChangedCallback(name, oldValue, newValue) {
		// console.log(`WebComponent(${this.constructor.name}).attributeChangedCallback`, "name", name, "oldValue", oldValue, "newValue", newValue);
		if (this.isConnected && newValue !== oldValue)
			this.requestDisplay();
	}

	requestDisplay(delay = 10) {
		// console.log(`WebComponent(${this.constructor.name}).requestUpdate`);
		const u = this.#displayUpdate;
		if (u.ongoing) {
			u.repeat = true;
			return;
		}

		if (typeof u.timeoutID === "number")
			clearTimeout(u.timeoutID);

		u.timeoutID = setTimeout(async () => await this.displayTimeout(), delay);
	}

	async displayTimeout() {
		// console.log(`WebComponent(${this.constructor.name}).displayTimeout`);
		const i = this.#initializeTemplating;
		if (i)
			await i;
		const u = this.#displayUpdate;
		u.timeoutID = undefined;
		if (!this.isConnected)
			return;
		u.ongoing = true;
		try {
			await this.updateDisplay();
		} finally {
			u.ongoing = false;
		}
		// console.log(`WebComponent(${this.constructor.name}).displayTimeout`, "u.repeat", u.repeat);
		if (u.repeat) {
			u.repeat = false;
			this.requestDisplay(0);
		}
	}

	async updateDisplay() {
		// console.log(`WebComponent(${this.constructor.name}).updateDisplay`);
		if (this.#templating)
			this.appendChild(this.interpolateDom({
				$template: "",
				...this.state
			}));
	}

	interpolateDom(input) {
		// console.log("WebComponent(${this.constructor.name}).interpolateDom");
		const getInterpolate = (template, index) => {
			const x = this.#templating[template];
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
			const y = Object.fromEntries(Object.entries(x).filter(([k, _]) => k !== "$template").map(([k, v]) => {
				const v2 = interpolate(v);
				return [k, v2];
			}));
			const t = x.$template;
			//if (t === "object")
			//debugger;
			indexes[t] ??= 0;
			const i = getInterpolate(t, indexes[t]++);
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

const compileNode = rootNode => {
	const path = [];
	const interpolatorBuilders = [];
	for (let node = rootNode; node;) {
		if (node instanceof Text) {
			const text = node.nodeValue;
			if (text.includes("${") && text.includes("}")) {
				const pathCopy = [...path];
				//node.nodeValue = "";
				const isTextAreaChild = node.parentElement instanceof HTMLTextAreaElement;
				interpolatorBuilders.push(rootNodeCopy => {
					const nodeCopy = findNode(rootNodeCopy, pathCopy);
					return context => {
						const value = text.replace(expressionRegex, (_, expression) => evaluate(expression, context) ?? "");
						if (isTextAreaChild) {
							const textArea = nodeCopy.parentElement;
							if (value !== textArea.value)
								textArea.value = value;
						} else if (value !== nodeCopy.nodeValue)
							nodeCopy.nodeValue = value;
					};
				});
			}
		} else if (node instanceof Comment) {
			const text = node.nodeValue;
			if (text.startsWith("${") && text.indexOf("}") === text.length - 1) {
				const pathCopy = [...path];
				const expression = text.substring(2, text.length - 1);
				//node.nodeValue = "";
				interpolatorBuilders.push(rootNodeCopy => {
					const nodeCopy = findNode(rootNodeCopy, pathCopy);
					return context => {
						const nodes1 = nodeCopy.insertedNodes ?? [];
						let referenceNode;
						for (let i = nodes1.length - 1; i >= 0; i--)
							if (nodes1[i].parentNode === nodeCopy.parentNode) {
								referenceNode = nodes1[i];
								break;
							}
						referenceNode = (referenceNode ?? nodeCopy).nextSibling;
						const value = evaluate(expression, context);
						const values = Array.isArray(value) ? value : value != null ? [value] : [];
						const nodes2 = values.flatMap(v => {
							if (v instanceof DocumentFragment) {
								const nodes0 = (v.originalChildNodes ??= [...v.childNodes]);
								if (!v.firstChild && nodes0.length) {
									for (let ps = nodes0[0].previousSibling; ps; ps = ps.previousSibling)
										if (ps instanceof Comment) {
											const nn1 = ps.insertedNodes;
											const nn2 = nn1.filter(x => !nodes0.includes(x));
											if (nn2.length < nn1.length)
												ps.insertedNodes = nn2;
										}
									return nodes0;
								}
								return [...v.childNodes];
							}
							if (v != null) {
								const t = document.createElement("template");
								t.innerHTML = v;
								return [...t.content.childNodes];
							}
							return v;
						});
						for (const n of nodes1)
							if (!nodes2.includes(n) && n.parentNode === nodeCopy.parentNode)
								n.parentNode.removeChild(n);
						for (let i = nodes2.length - 1; i >= 0; i--) {
							const n = nodes2[i];
							if (n !== referenceNode?.previousSibling)
								nodeCopy.parentNode.insertBefore(n, referenceNode);
							referenceNode = n;
						}
						nodeCopy.insertedNodes = nodes2;
					};
				});
			}
		} else if (node instanceof Element && node.hasAttributes()) {
			let i = 0;
			for (const attribute of node.attributes) {
				const text = attribute.value;
				if (text.includes("${") && text.includes("}")) {
					const pathCopy = [...path, i];
					//a.value = "";
					const isExpression = text.startsWith("${") && text.indexOf("}") === text.length - 1;
					interpolatorBuilders.push(rootNodeCopy => {
						const attributeCopy = findNode(rootNodeCopy, pathCopy, true);
						const element = attributeCopy.ownerElement;
						return context => {
							let value;
							const stringValue = text.replace(expressionRegex, (_, expression) => {
								value = evaluate(expression, context);
								return value ?? "";
							});
							// console.log("text", text, "isExpression", isExpression, "value", value, "stringValue", stringValue);
							if (!isExpression)
								;
							else if (value == null || value === false)
								attributeCopy.ownerElement?.removeAttributeNode(attributeCopy);
							else if (!attributeCopy.ownerElement)
								element.setAttributeNode(attributeCopy);
							if (stringValue !== attributeCopy.value)
								attributeCopy.value = value === true ? "" : stringValue;
							switch (attributeCopy.name) {
								case "checked":
									if (element instanceof HTMLInputElement) {
										if (element.checked !== value)
											element.checked = value;
									}
									break;
								case "value":
									if (element instanceof HTMLInputElement || element instanceof HTMLSelectElement || element instanceof HTMLTextAreaElement) {
										if (!attributeCopy.ownerElement)
											delete element.value;
										else if (element.value !== attributeCopy.value)
											element.value = attributeCopy.value;
									}
									break;
							}
						};
					});
				}
				i++;
			}
		}

		if (node.firstChild) {
			node = node.firstChild;
			path.push(0);
		} else
			do
				if (node === rootNode)
					node = null;
				else if (node.nextSibling) {
					node = node.nextSibling;
					path[path.length - 1]++;
					break;
				} else {
					node = node.parentNode;
					path.pop();
				}
			while (node);
	}
	return () => {
		const rootNodeCopy = rootNode.cloneNode(true);
		if (rootNodeCopy instanceof DocumentFragment)
			rootNodeCopy.originalChildNodes = [...rootNodeCopy.childNodes];
		const interpolators = interpolatorBuilders.map(build => build(rootNodeCopy));
		return context => {
			interpolators.forEach(interpolate => interpolate(context));
			return rootNodeCopy;
		};
	};
};
