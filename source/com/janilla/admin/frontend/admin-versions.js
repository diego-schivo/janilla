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
import WebComponent from "web-component";

export default class AdminVersions extends WebComponent {

    static get templateNames() {
        return ["admin-versions"];
    }

    constructor() {
        super();
    }

    async updateDisplay() {
        const a = this.closest("admin-element");
        const hh = ["updatedAt", "id", "documentStatus"];
        this.appendChild(this.interpolateDom({
            $template: "",
            heads: hh.map(x => ({
                $template: "head",
                text: x.split(/(?=[A-Z])/).map(y => y.charAt(0).toUpperCase() + y.substring(1)).join(" ")
            })),
            rows: a.state.versions.map(v => ({
                $template: "row",
                cells: hh.map(k => [(k.startsWith("document") ? v.document : v)[k]]
                    .map(x => {
                        switch (k) {
                            case "documentStatus":
                                return x.name;
                            case "updatedAt":
                                return a.dateTimeFormat.format(new Date(x));
                            default:
                                return x;
                        }
                    })
                [0]).map((x, i) => ({
                    $template: "cell",
                    content: i === 0 ? {
                        $template: "link",
                        href: `/admin${a.dataset.path}/${v.id}`,
                        content: x
                    } : x
                }))
            }))
        }));
    }
}
