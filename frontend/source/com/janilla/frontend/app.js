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
