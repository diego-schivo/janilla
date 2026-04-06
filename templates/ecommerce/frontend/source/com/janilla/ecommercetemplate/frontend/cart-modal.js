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

export default class CartModal extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["cart-modal"];
    }

    connectedCallback() {
        super.connectedCallback();
        addEventListener("cartchanged", this.handleCartChanged);
        this.addEventListener("click", this.handleClick);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        removeEventListener("cartchanged", this.handleCartChanged);
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        const a = this.closest("app-element");
        const c = localStorage.getItem("cart");
        const u = new URL(`${a.dataset.apiUrl}/carts/${c}`, location.href);
        if (!a.currentUser)
            u.searchParams.append("secret", localStorage.getItem("cart_secret"));
        s.cart ??= (c ? await (await fetch(u)).json() : null) ?? {};
        if (c && !s.cart.id)
            localStorage.removeItem("cart");
        const q = s.cart.items?.reduce((x, y) => x + y.quantity, 0) ?? 0;
        this.appendChild(this.interpolateDom({
            $template: "",
            quantity: q !== 0 ? {
                $template: "quantity",
                text: q
            } : null,
            content: q !== 0 ? {
                $template: "content",
                items: s.cart.items.map(x => ({
                    $template: "item",
                    ...x,
                    image: x.product.gallery.find(y => x.variant.options.some(z => z.id === y.variantOption.id)).image,
                    option: x.variant.options.map(y => y.label).join(", "),
                    quantityMinus1: x.quantity - 1,
                    quantityPlus1: x.quantity + 1,
                }))
            } : { $template: "empty-content" },
            footer: q !== 0 ? {
                $template: "footer",
                total: s.cart.subtotal
            } : null
        }));
    }

    handleCartChanged = event => {
		this.customState.cart = event.detail;
		this.requestDisplay();
    }

    handleClick = event => {
        const x = event.target.closest("button");
        switch (x?.name) {
            case "show":
                this.querySelector("dialog").showModal();
                break;
            case "close":
                this.querySelector("dialog").close();
                break;
        }
    }

    handleSubmit = async event => {
        event.preventDefault();
        const s = this.customState;
        const fd = new FormData(event.target);
        const p = parseInt(fd.get("product"));
        const v = parseInt(fd.get("variant"));
        const q = parseInt(fd.get("quantity"));
        const r = await fetch(`${this.closest("app-element").dataset.apiUrl}/carts/${s.cart.id}`, {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
                items: s.cart.items.map(x => ({
                    $type: "CartItem",
                    product: x.product.id,
                    variant: x.variant.id,
                    quantity: x.product.id == p && x.variant.id == v ? q : x.quantity
                })).filter(x => x.quantity)
            })
        });
        s.cart = await r.json();
        /*
        this.dispatchEvent(new CustomEvent("cart-change", {
            bubbles: true,
            detail: j
        }));
        */
        this.requestDisplay();
    }
}
