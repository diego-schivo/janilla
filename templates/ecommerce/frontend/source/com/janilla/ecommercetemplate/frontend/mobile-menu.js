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
import WebComponent from "base/web-component";

export default class MobileMenu extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["mobile-menu"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        (this.customState.app = this.closest("app-element")).addEventListener("userchanged", this.handleUserChanged);
    }

    disconnectedCallback() {
        const a = this.customState.app;
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        a.removeEventListener("userchanged", this.handleUserChanged);
    }

    async updateDisplay() {
        const a = this.closest("app-element");
        this.appendChild(this.interpolateDom({
            $template: "",
            storeItems: a.customState.header?.navItems?.map(x => ({
                $template: "list-item",
                ...x,
                document: x.type.name === "REFERENCE" ? `${x.document.$type}:${x.document.slug}` : null,
                href: x.type.name === "CUSTOM" ? x.uri : null,
                target: x.newTab ? "_blank" : null
            })),
            accountItems: (a.currentUser ? [{
                href: "/orders",
                text: "Orders"
            }, {
                href: "/account/addresses",
                text: "Addresses"
            }, {
                href: "/account",
                text: "Manage account"
            }, {
                href: "/logout",
                class: "button secondary",
                text: "Log out"
            }] : [{
                href: "/login",
                class: "button secondary",
                text: "Log in"
            }, {
                href: "/create-account",
                class: "button primary",
                text: "Create an account"
            }]).map(x => ({
                $template: "list-item",
                ...x
            }))
        }));
    }

    handleClick = event => {
        const el = event.target;

        switch (el.closest("button")?.name) {
            case "show":
                this.querySelector("dialog").showModal();
                break;
            case "close":
                this.querySelector("dialog").close();
                break;
        }

        if (el.matches("dialog"))
            this.querySelector("dialog").close();
    }

    handleUserChanged = () => {
        this.requestDisplay();
    }
}
