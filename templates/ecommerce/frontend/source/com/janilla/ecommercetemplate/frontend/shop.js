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

export default class Shop extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["shop"];
    }

    static get observedAttributes() {
        return ["data-category", "data-query", "data-sort"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const a = this.closest("app-element");
        let hs = history.state;
        let cc = a.serverState?.categories ?? hs.categories;
        let pp = a.serverState?.products ?? hs.products;
        if (!pp)
            [cc, pp] = await Promise.all([
                fetch(`${a.dataset.apiUrl}/categories`).then(async x => (await x.json()).elements),
                (() => {
                    const u = new URL(`${a.dataset.apiUrl}/products`, location.href);
                    if (this.dataset.query)
                        u.searchParams.append("q", this.dataset.query);
                    if (this.dataset.category)
                        u.searchParams.append("category", this.dataset.category);
                    if (this.dataset.sort)
                        u.searchParams.append("sort", this.dataset.sort);
                    u.searchParams.append("depth", 1);
                    return fetch(u).then(async x => (await x.json()).elements);
                })()
            ]);
        history.replaceState(hs = {
            ...hs,
            categories: cc,
            products: pp
        }, "");
        a.updateSeo({ title: "Shop" });
        this.appendChild(this.interpolateDom({
            $template: "",
            ...this.dataset,
            categoryItems: hs.categories.sort((x, y) => x.title < y.title ? -1 : x.title > y.title ? 1 : 0).map(x => ({
                $template: "item",
                name: "category",
                value: x.id,
                checked: x.id == this.dataset.category,
                text: x.title
            })),
            sortOptions: [
                [undefined, "Alphabetic A-Z"],
                ["-createdAt", "Latest arrivals"],
                ["priceInUSD", "Price: Low to high"],
                ["-priceInUSD", "Price: High to low"]
            ].map(([k, v]) => ({
                $template: "option",
                value: k ?? "",
                selected: k == this.dataset.sort,
                text: v
            })),
            sortItems: [
                [undefined, "Alphabetic A-Z"],
                ["-createdAt", "Latest arrivals"],
                ["priceInUSD", "Price: Low to high"],
                ["-priceInUSD", "Price: High to low"]
            ].map(([k, v]) => ({
                $template: "item",
                name: "sort",
                value: k ?? "",
                checked: k == this.dataset.sort,
                text: v
            })),
            products: hs.products.map(x => ({
                $template: "product",
                ...x
            }))
        }));
    }

    handleChange = async event => {
        const el = event.target;
        Array.from(el.form.elements[el.name]).filter(x => x.matches('[type="checkbox"]') && x !== el).forEach(x => x.checked = false);
        el.form.requestSubmit();
    }

    handleSubmit = async event => {
        const el = event.target;
        event.preventDefault();
        const u = new URL("/shop", location.href);
        Array.from(new FormData(el).entries()).filter(([_, v]) => v).forEach(([k, v]) => u.searchParams.append(k, v));
        a.navigate(u);
    }
}
