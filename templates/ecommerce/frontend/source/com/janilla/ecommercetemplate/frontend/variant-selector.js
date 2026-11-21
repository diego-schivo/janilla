/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Payload CMS, LLC <info@payloadcms.com>
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

export default class VariantSelector extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["variant-selector"];
    }

    static get observedAttributes() {
        return ["data-time"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
    }

    async updateDisplay() {
        const p = history.state.product;
        const tt = p.variantTypes
            .sort((a, b) => a.label < b.label ? -1 : a.label > b.label ? 1 : 0);

        const pe = this.closest("product-element");
        const ii = pe.activeOptions.map(x => x.id);
        const oo = Object.fromEntries(tt
            .map(t => [t.name, t.options.find(o => ii.includes(o.id))])
            .filter(x => x[1]));
        this.appendChild(this.interpolateDom({
            $template: "",
            types: tt.map(t => ({
                $template: "type",
                ...t,
                options: t.options.map(o => ({
                    $template: "option",
                    name: t.name,
                    ...o,
                    checked: ii.includes(o.id),
                    disabled: p.variants
                        .filter(x =>
                            x.options.every(y => {
                                if (y.type.id === t.id)
                                    return y.id === o.id;
                                if (!Object.hasOwn(oo, y.type.name))
                                    return true;
                                return y.id === oo[y.type.name].id;
                            }))
                        .every(x => !x.inventory)
                }))
            }))
        }));
    }

    handleChange = event => {
        const el = event.target;
        const p = history.state.product;
        const t = p.variantTypes.find(x => x.name === el.name);
        const o = t.options.find(x => x.id == el.value);
        dispatchEvent(new CustomEvent("optionselected", { detail: o }));
    }
}
