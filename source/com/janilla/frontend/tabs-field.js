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
export default class TabsField extends HTMLElement {

	constructor() {
		super();
	}

	connectedCallback() {
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		const b = event.target.closest("button");
		if (!b)
			return;
		event.stopPropagation();
		Array.prototype.forEach.call(b.parentElement.children, x => x.ariaSelected = x === b);
		const ac = b.getAttribute("aria-controls");
		this.querySelectorAll(':scope > [role="tabpanel"]').forEach(x => {
			if (x.id === ac)
				x.removeAttribute("hidden");
			else
				x.setAttribute("hidden", "");
		});
	}
}
