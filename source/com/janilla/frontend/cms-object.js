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
import { WebComponent } from "./web-component.js";

export default class CmsObject extends WebComponent {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "cms-object";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const p = this.dataset.path;
		const f = a.field(p);
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			...f,
			lists: (() => {
				const pp0 = Object.entries(f.properties).filter(([k, _]) => k !== "id");
				let snn = a.sidebar(f.type);
				return (snn?.length ? [
					pp0.filter(([k, _]) => !snn.includes(k)),
					pp0.filter(([k, _]) => snn.includes(k))
				] : [pp0]).map(pp => ({
					$template: "list",
					items: (() => {
						const ff = pp.map(([k, v]) => {
							const ct = a.controlTemplate(a.field(k, f));
							const p2 = p ? `${p}.${k}` : k;
							return {
								$template: ["cms-select", "cms-text", "textarea-control"].includes(ct) ? "label-nest-field"
									: ["checkbox-control"].includes(ct) ? "no-label-field"
										: "label-field",
								label: a.label(p2),
								name: k,
								control: {
									$template: ct,
									path: p2,
									key: this.dataset.key,
									...v
								}
							};
						});
						let tnn = a.tabs(f.type);
						let ii;
						if (tnn) {
							const kk = pp.map(([k, _]) => k);
							ii = Object.fromEntries(Object.values(tnn).flatMap(x => x.map(y => [y, kk.indexOf(y)])).filter(([_, i]) => i !== -1));
							if (!Object.keys(ii).length)
								tnn = null;
						}
						const jj = (tnn ? ff.filter(x => !Object.hasOwn(ii, x.name)) : ff).map(x => ({
							$template: "item",
							field: x
						}));
						if (tnn)
							jj.splice(Object.values(ii)[0], 0, {
								$template: "item",
								field: {
									$template: "tabs",
									name: f.type,
									tabs: Object.keys(tnn).join(),
									activeTab: Object.keys(tnn)[0],
									items: Object.entries(tnn).map(([k, v]) => ({
										$template: "tabs-item",
										tab: k,
										panel: {
											$template: "tabs-panel",
											tab: k,
											items: (() => {
												return v.map(x => ({
													$template: "item",
													field: ff.find(y => y.name === x)
												}));
											})()
										}
									}))
								}
							});
						return jj;
					})()
				}));
			})()
		}));
	}
}
