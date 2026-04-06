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

export default class AdminVariantOptions extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-variant-options"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
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
        const e = this.closest("admin-edit");
        const p = this.dataset.path;
        const s = this.customState;
        s.field ??= e.field(p);
        s.values ??= (s.field.data ?? []).map(x => x.id);
        this.appendChild(this.interpolateDom({
            $template: "",
            types: e.customState.document.product.variantTypes
                .sort((a, b) => a.label < b.label ? -1 : a.label > b.label ? 1 : 0)
                .map(x => ({
                    $template: "type",
                    ...x,
                    options: x.options.map(y => ({
                        $template: "option",
                        value: y.id,
                        selected: s.values.some(z => z === y.id),
                        text: y.label
                    }))
                }))
        }));
    }

    handleClick = event => {
        const el = event.target.closest("button");
        const s = this.customState;
        switch (el?.name) {
            case "remove":
                s.values = Array.from(this.querySelectorAll("select"))
                    .filter(x => !x.contains(el))
                    .map(x => x.value);
                this.requestDisplay();
                break;
        }
    }
}
