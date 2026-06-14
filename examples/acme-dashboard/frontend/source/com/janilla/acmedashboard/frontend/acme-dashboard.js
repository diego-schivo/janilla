/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import WebComponent from "base/web-component";

export default class AcmeDashboard extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["acme-dashboard"];
    }

    constructor() {
        super();

        this.attachShadow({ mode: "open" });
    }

    async updateDisplay() {
        const a = this.closest("app-element");
        const p = a.currentPath;
		const f = this.interpolateDom({
		    $template: "",
		    welcome: {
		        $template: "welcome",
		        slot: p === "/" ? "content" : null
		    },
		    login: {
		        $template: "login",
		        slot: p === "/login" ? "content" : null
		    },
		    dashboard: (() => {
		        const d = p.split("/")[1] === "dashboard";
		        return {
		            $template: "dashboard",
		            slot: d ? "content" : null,
		            uri: d ? p + location.search : null
		        };
		    })()
		});
		this.shadowRoot.append(...f.querySelectorAll("slot"));
		this.appendChild(f);
    }
}
