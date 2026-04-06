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

export default class AddressItem extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["address-item"];
    }

    static get observedAttributes() {
        return ["data-id", "data-data", "data-hide-actions"];
    }

    constructor() {
        super();
        this.attachShadow({ mode: "open" });
    }

    async updateDisplay() {
        const s = this.customState;
        const a = this.closest("app-element");
        s.address ??= this.dataset.data
            ? JSON.parse(this.dataset.data)
            : a.currentUser.addresses.find(x => x.id == this.dataset.id);
        this.shadowRoot.appendChild(this.interpolateDom({
            $template: "",
            lines: [
                [
                    [s.address.title]
                        .map(x => typeof x === "object" ? x?.name : x)
                        .map(x => x !== "OTHER" ? x : null)
                    [0],
                    s.address.firstName,
                    s.address.lastName
                ].filter(x => x).join(" "),
                s.address.phone,
                [
                    s.address.addressLine1,
                    s.address.addressLine2
                ].filter(x => x).join(", "),
                [
                    s.address.city,
                    s.address.customState,
                    s.address.postalCode
                ].filter(x => x).join(", "),
                [s.address.country]
                    .map(x => typeof x === "object" ? x?.name : x)
                [0],
            ].filter(x => x).map(x => ({
                $template: "line",
                text: x
            })),
            actions: this.dataset.hideActions === undefined ? { $template: "actions" } : null
        }));
    }
}
