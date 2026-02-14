/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
import WebComponent from "./web-component.js";

export default class App extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["app"];
    }

    constructor() {
        super();
        const el = this.children.length === 1 ? this.firstElementChild : null;
        if (el?.matches('[type="application/json"]')) {
            this.serverState = JSON.parse(el.text);
            el.remove();
        }
        if (!history.state || this.serverState)
            history.replaceState({}, "");
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        addEventListener("popstate", this.handlePopState);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        removeEventListener("popstate", this.handlePopState);
    }

    async updateDisplay() {
        const s = this.customState;
        const ss = this.serverState;

        if (ss?.error?.code === 404)
            s.notFound = true;

        await this.updateDisplaySite();
    }

    async updateDisplaySite() {
        this.appendChild(this.interpolateDom({
            $template: "",
            site: this.customState.notFound ? { $template: "not-found" } : { $template: "page" }
        }));
    }

    handleClick = event => {
        if (!event.defaultPrevented) {
            const a = event.target.shadowRoot
                ? event.composedPath().find(x => x instanceof Element && x.matches("a"))
                : event.target.closest("a");
            const u = a?.href && !a.target ? new URL(a.href) : null;
            if (u?.origin === location.origin) {
                event.preventDefault();
                this.navigate(u);
            }
        }
    }

    handlePopState = () => {
        // console.log("handlePopState", JSON.stringify(history.state));
        this.navigate();
    }

    navigate(url) {
        delete this.serverState;
        delete this.customState.notFound;
        if (!url || url.pathname !== location.pathname)
            window.scrollTo(0, 0);
        if (url) {
            history.pushState({}, "", url.pathname + url.search);
            dispatchEvent(new Event("statepushed"));
        }
        this.requestDisplay();
    }

    notFound() {
        this.customState.notFound = true;
        this.requestDisplay();
    }
}
