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
export default class WebComponent extends HTMLElement {

	#initializeTemplating;

	#templating;

	#displayUpdate = {
		ongoing: false,
		repeat: false,
		timeoutID: undefined
	};

	constructor() {
		super();
		if (!this.constructor.templateNames?.length)
			return;
		this.#initializeTemplating = Promise.all(this.constructor.templateNames.map(x => getDocumentFragment(x))).then(x => {
			const documentFragment = x.reduce((y, z) => {
				y.appendChild(z.cloneNode(true));
				return y;
			}, new DocumentFragment());
			const templates = [...documentFragment.querySelectorAll("template")].map(y => {
				y.remove();
				return y;
			});
			this.#templating = Object.fromEntries([
				["", compileNode(documentFragment)],
				...templates.map(y => [y.id, compileNode(y.content)])
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

	requestDisplay(delay = 1) {
		// console.log(`WebComponent(${this.constructor.name}).requestDisplay`);
		if (this.#displayUpdate.ongoing) {
			this.#displayUpdate.repeat = true;
			return;
		}

		if (typeof this.#displayUpdate.timeoutID === "number")
			clearTimeout(this.#displayUpdate.timeoutID);

		this.#displayUpdate.timeoutID = setTimeout(async () => await this.displayTimeout(), delay);
	}

	async displayTimeout() {
		// console.log(`WebComponent(${this.constructor.name}).displayTimeout`);
		if (this.#initializeTemplating)
			await this.#initializeTemplating;
		this.#displayUpdate.timeoutID = undefined;
		if (!this.isConnected)
			return;
		this.#displayUpdate.ongoing = true;
		try {
			await this.updateDisplay();
		} finally {
			this.#displayUpdate.ongoing = false;
		}
		if (this.#displayUpdate.repeat) {
			this.#displayUpdate.repeat = false;
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
			indexes[x.$template] ??= 0;
			return getInterpolate(x.$template, indexes[x.$template]++)(y);
		};
		const node = interpolate(input);
		for (const [k, v] of Object.entries(this.#templating)) {
			const length = indexes[k] ?? 0;
			if (v.functions.length > length)
				v.functions.length = length;
		}
		return node;
	}
}

const documentFragments = {};

const getDocumentFragment = async name => {
	documentFragments[name] ??= fetch(`/${name}.html`).then(x => {
		if (!x.ok)
			throw new Error(`Failed to fetch /${name}.html: ${x.status} ${x.statusText}`);
		return x.text();
	}).then(x => {
		const template = document.createElement("template");
		template.innerHTML = x;
		return template.content;
	});
	return await documentFragments[name];
};

const evaluate = (expression, context) => {
	let value = context;
	if (expression)
		for (const subexpr of expression.split(".")) {
			if (value == null)
				break;
			const index = subexpr.endsWith("]") ? subexpr.indexOf("[") : -1;
			value = index === -1 ? value[subexpr] : value[subexpr.substring(0, index)]?.[parseInt(subexpr.substring(index + 1, subexpr.length - 1))];
		}
	return value;
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
								case "selected":
									if (element instanceof HTMLOptionElement && value) {
										const pe = element.parentElement;
										if (pe instanceof HTMLSelectElement && pe.value !== element.value)
											pe.value = element.value;
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
