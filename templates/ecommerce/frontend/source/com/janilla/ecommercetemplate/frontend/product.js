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

export default class Product extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["product"];
    }

    static get observedAttributes() {
        return ["data-time"];
    }

    get activeOptions() {
        const p = history.state.product;
        if (!p.enableVariants)
            return [];
        const pp = new URLSearchParams(location.search);
        return p.variantTypes.map(t => {
            if (!pp.has(t.name))
                return undefined;
            const i = parseInt(pp.get(t.name));
            return t.options.find(x => x.id === i);
        }).filter(x => x);
    }

    get activeVariant() {
        const p = history.state.product;
        const pp = new URLSearchParams(location.search);
        if (!p.enableVariants || !pp.has("variant"))
            return undefined;

        const i = parseInt(pp.get("variant"));
        return p.variants.find(x => x.id === i);
    }

    connectedCallback() {
        super.connectedCallback();
        addEventListener("optionselected", this.handleOptionSelected);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        removeEventListener("optionselected", this.handleOptionSelected);
    }

    async updateDisplay() {
        let hs = history.state;
        const a = this.closest("app-element");

        if (!Object.hasOwn(hs, "product"))
            history.replaceState(hs = {
                ...hs,
                product: a.serverState?.product
            }, "");

        const s = a.currentPath.split("/")[2];
        if (s !== hs.product?.slug) {
            const u = new URL(`${a.customEnv.apiUrl}/products`, location.href);
            u.searchParams.append("slug", s);
            u.searchParams.append("depth", 3);
            const p = (await (await fetch(u)).json()).elements[0];
            history.replaceState(hs = {
                ...hs,
                product: p
            }, "");
        }

        a.updateSeo(hs.product.meta);
        this.appendChild(this.interpolateDom({
            ...a.baseInput,
            $template: "",
            time: new Date().getTime(),
            layout: hs.product.layout?.map((x, i) => ({
                $template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
                path: `layout.${i}`
            }))
        }));
    }

    findData(path) {
        return path.split(".").reduce((x, n) => Array.isArray(x)
            ? x[parseInt(n)]
            : typeof x === "object" && x !== null
                ? x[n]
                : null, history.state.product);
    }

    findVariant(options) {
        const p = history.state.product;
        const ii = options.map(x => typeof x === "number" ? x : x.id);
        return p.variants.find(v => v.options.every(o => ii.includes(o.id)));
    }

	handleOptionSelected = event => {
	    const o = event.detail;

	    const oo = this.activeOptions;
	    const i = oo.findIndex(x => x.type.id === o.type.id);
	    if (i !== -1)
	        oo[i] = o;
	    else
	        oo.push(o);
	    const v = this.findVariant(oo);

	    const u = new URL(location.href);
	    u.searchParams.set(o.type.name, o.id);
	    if (v)
	        u.searchParams.set("variant", v.id);
	    else
	        u.searchParams.delete("variant");

	    this.closest("app-element").navigateTo(u)
	}
}
