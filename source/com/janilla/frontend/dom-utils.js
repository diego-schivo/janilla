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
export const removeAllChildren = element => {
	while (element.firstChild)
		element.removeChild(element.lastChild);
};

export const matchNode = (xpath, context, not) => {
	const q = resolve => {
		const n = context.ownerDocument.evaluate(xpath, context, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue
		if (not ? !n : n) {
			// console.log("matchNode", xpath, n);
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
