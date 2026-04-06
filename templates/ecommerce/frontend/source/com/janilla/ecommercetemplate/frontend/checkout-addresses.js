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

export default class CheckoutAddresses extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["checkout-addresses"];
    }

    static get observedAttributes() {
        return ["data-heading", "data-description", "data-name"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
    }

    async updateDisplay() {
        const a = this.closest("app-element");
        const s = this.customState;
        this.appendChild(this.interpolateDom({
            $template: "",
            ...this.dataset,
            ...this.customState,
            items: a.currentUser.addresses.map(x => ({
                $template: "item",
                id: x.id
            }))
        }));
        const d = this.querySelector("dialog");
        if (s.dialog)
            d.showModal();
        else
            d.close();
    }

    handleClick = event => {
        const b = event.target.closest("button");
        const s = this.customState;
        switch (b?.name) {
            case "close":
                delete s.dialog;
                this.requestDisplay();
                break;
            case "open":
                s.dialog = true;
                this.requestDisplay();
                break;
            case "select":
                /*
                this.dispatchEvent(new CustomEvent("addressselected", {
                    bubbles: true,
                    detail: { id: parseInt(b.value) }
                }));
                */
                this.customState.value = b.value;
                const i = this.querySelector(`input[name="${this.dataset.name}"]`);
                i.value = b.value;
                i.dispatchEvent(new Event("change", { bubbles: true }));
                break;
        }
    }
}
