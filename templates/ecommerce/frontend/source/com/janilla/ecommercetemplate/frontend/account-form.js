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

export default class AccountForm extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["account-form"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        this.addEventListener("input", this.handleInput);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("input", this.handleInput);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        const a = this.closest("app-element");

        this.appendChild(this.interpolateDom({
            $template: "",
            content: s.changePassword ? {
                $template: "change-password",
                ...Object.fromEntries(Object.entries(s.errors ?? {}).map(([x, y]) => [x + "Error", {
                    $template: "error",
                    text: y
                }]))
            } : {
                $template: "update-account",
                user: a.currentUser
            }
        }));
    }

    handleClick = event => {
        const el = event.target.closest("a");
        const s = this.customState;
        switch (el?.getAttribute("href")) {
            case "#change-password":
                event.preventDefault();
                s.changePassword = true;
                this.requestDisplay();
                break;
            case "#update-account":
                event.preventDefault();
                s.changePassword = false;
                this.requestDisplay();
                break;
        }
    }

    handleInput = event => {
        const o = Object.fromEntries(new FormData(event.target.form));
        const u = this.closest("app-element").currentUser;
        this.querySelector("button").disabled = this.customState.changePassword
            ? o.password === '' && o.passwordConfirm === ''
            : o.email === u.email && o.name === u.name;
    }

    handleSubmit = async event => {
        const el = event.target.closest("form");
        event.preventDefault();

        const a = this.closest("app-element");
        const s = this.customState;
        const o = Object.fromEntries(new FormData(el));

        s.errors = Object.fromEntries(Object.entries(s.changePassword ? {
            password: o.password === "" ? "Please provide a new password." : null,
            passwordConfirm: o.passwordConfirm !== o.password ? "The passwords do not match" : null
        } : {}).filter(([_, x]) => x));

        if (Object.keys(s.errors).length)
            this.requestDisplay();
        else {
            const r = await fetch(`/api/users/${a.currentUser.id}`, {
                method: "PATCH",
                headers: { "content-type": "application/json" },
                body: JSON.stringify({
                    $type: "User",
                    ...o
                })
            });
            const j = await r.json();

            if (r.ok) {
                a.success("Successfully updated account.");
                this.dispatchEvent(new CustomEvent("userchanged", {
                    bubbles: true,
                    detail: j
                }));
                this.customState.changePassword = false;
                this.requestDisplay();
            } else
                a.error(j);
        }
    }
}
