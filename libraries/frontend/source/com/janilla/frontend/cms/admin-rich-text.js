/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
import WebComponent from "base/web-component";

export default class AdminRichText extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-rich-text"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        //this.addEventListener("keyup", this.handleKeyUp);
        this.addEventListener("input", this.handleInput);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        //this.removeEventListener("keyup", this.handleKeyUp);
        this.removeEventListener("input", this.handleInput);
    }

    async updateDisplay() {
		const s = this.customState;
        const p = this.dataset.path;
        s.field ??= this.closest("admin-edit").field(p);
        const d = this.querySelector("div");
        const i = this.querySelector("input");
        const v = i?.value;
        this.appendChild(this.interpolateDom({
            $template: "",
            items: ["type", "heading-1", "heading-2", "heading-3", "heading-4", "bold", "italic", "underline", "link"].map(x => ({
                $template: "item",
                name: x,
                class: ["bold", "italic", "underline"].includes(x) && document.queryCommandState(x) ? "active" : null
            })),
			input: {
				$template: "input",
				name: p,
				value: d?.innerHTML ?? s.field.data
			}
        }));
        if (!d)
            this.querySelector("div").innerHTML = s.field.data ?? "";
        if (i && i.value !== v)
            i.dispatchEvent(new InputEvent("input", {
                view: window,
                bubbles: true,
                cancelable: true
            }));
    }

    handleClick = event => {
        const b = event.target.closest("button");
        if (b) {
            switch (b.name) {
                case "type":
                    document.execCommand("formatBlock", false, "<p>");
                    break;
                case "heading-1":
                case "heading-2":
                case "heading-3":
                case "heading-4":
                    document.execCommand("formatBlock", false, `<h${b.name.charAt(b.name.length - 1)}>`);
                    break;
                case "bold":
                case "italic":
                case "underline":
                    document.execCommand(b.name);
                    break;
                case "link":
                    const u = prompt("URL");
                    if (u)
                        document.execCommand("createLink", false, u);
                    break;
            }
            this.requestDisplay();
        }

        /*
        const ce = event.target.closest("[contenteditable]");
        if (ce)
            this.requestDisplay();
        */
    }

    /*
	handleKeyUp = event => {
        const ce = event.target.closest("[contenteditable]");
        if (ce)
            this.requestDisplay();
    }
	*/

    handleInput = event => {
        const d = event.target.closest("div");
        if (d) {
            if (!d.firstElementChild)
                document.execCommand("formatBlock", false, "<p>");
            this.requestDisplay();
        }
    }
}
