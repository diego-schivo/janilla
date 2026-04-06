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

export default class Checkout extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["checkout"];
    }

    set paymentData(paymentData) {
        this.customState.paymentData = paymentData;
        this.requestDisplay();
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("click", this.handleClick);
        this.addEventListener("input", this.handleInput);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("input", this.handleInput);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const a = this.closest("app-element");
        const s = this.customState;
        s.guestEmailEditable ??= !a.currentUser;
        if (s.billingAddress === undefined) {
            const aa = a.currentUser?.addresses;
            s.billingAddress = aa?.length ? aa[aa.length - 1].id : null;
        }
        s.billingAddressSameAsShipping ??= true;
        const c = localStorage.getItem("cart");
        const u = new URL(`${a.dataset.apiUrl}/carts/${c}`, location.href);
        if (!a.currentUser)
            u.searchParams.append("secret", localStorage.getItem("cart_secret"));
        s.cart ??= await (await fetch(u)).json();
        //r.updateSeo(null);
        this.appendChild(this.interpolateDom({
            $template: "",
            contact: a.currentUser ? {
                $template: "user",
                text: a.currentUser.email
            } : {
                $template: "guest",
                value: s.guestEmail,
                disabled: !s.guestEmail || !s.guestEmailEditable
            },
            billingAddress: s.billingAddress ? {
                $template: "address-item",
                ...(typeof s.billingAddress === "object"
                    ? { data: JSON.stringify(s.billingAddress) }
                    : { id: s.billingAddress }),
                name: "billingAddress",
                disabled: s.paymentData
            } : a.currentUser ? {
                $template: "checkout-addresses",
                heading: "Billing address",
                description: "Please select or add your shipping and billing addresses.",
                name: "billingAddress"
            } : {
                $template: "create-address-modal",
                name: "billingAddress",
                disabled: !s.guestEmail || s.guestEmailEditable
            },
            billingAddressSameAsShipping: s.billingAddressSameAsShipping,
            billingAddressSameAsShippingDisabled: !s.billingAddress || s.paymentData,
            shippingAddress: s.billingAddressSameAsShipping ? null : s.shippingAddress ? {
                $template: "address-item",
                ...(typeof s.shippingAddress === "object"
                    ? { data: JSON.stringify(s.shippingAddress) }
                    : { id: s.shippingAddress }),
                name: "shippingAddress",
                disabled: s.paymentData
            } : a.currentUser ? {
                $template: "checkout-addresses",
                heading: "Shipping address",
                description: "Please select a shipping address.",
                name: "shippingAddress"
            } : {
                $template: "create-address-modal",
                name: "shippingAddress",
                disabled: !s.guestEmail || s.guestEmailEditable
            },
            payment: !s.paymentData ? {
                $template: "payment-trigger",
                disabled: !s.billingAddress
            } : {
                $template: "payment"
                //guestEmail: s.guestEmail,
                //amount: s.cart.subtotal * 100
            },
            items: s.cart.items.map(x => ({
                $template: "item",
                item: JSON.stringify(x)
            })),
            total: s.cart.subtotal
        }));
    }

    handleChange = event => {
        const el = event.target;
        const s = this.customState;
        switch (el?.name) {
            case "billingAddress":
                s.billingAddress = parseInt(el.value);
                this.requestDisplay();
                break;
            case "billingAddressSameAsShipping":
                s.billingAddressSameAsShipping = el.checked;
                this.requestDisplay();
                break;
            case "shippingAddress":
                s.shippingAddress = parseInt(el.value);
                this.requestDisplay();
                break;
        }
    }

    handleClick = async event => {
        const el = event.target;
        const s = this.customState;
        switch (el?.name) {
            case "billingAddress":
                s.billingAddress = null;
                this.requestDisplay();
                break;
            case "continue-as-guest":
                s.guestEmailEditable = false;
                this.requestDisplay();
                break;
            case "go-to-payment":
                const a = this.closest("app-element");
                const [ba, sa] = [s.billingAddress, s.billingAddressSameAsShipping ? s.billingAddress : s.shippingAddress]
                    .map(x => typeof x === "object" ? x : a.currentUser.addresses.find(y => y.id === x));
                const r = await fetch(`${a.dataset.apiUrl}/payments/stripe/initiate`, {
                    method: "POST",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({
                        guestEmail: s.guestEmail,
                        cart: s.cart.id,
                        billingAddress: ba,
                        shippingAddress: sa
                    })
                });
                const j = await r.json();
                if (r.ok)
                    this.paymentData = j;
                else
                    a.error(j);
                break;
            case "shippingAddress":
                s.shippingAddress = null;
                this.requestDisplay();
                break;
        }
    }

    handleInput = event => {
        const t = event.target;
        const s = this.customState;
        if (t.matches('[name="guestEmail"]')) {
            s.guestEmail = t.value;
            this.requestDisplay();
        }
    }

    handleSubmit = event => {
        const f = event.target;
        const s = this.customState;

        if (f.closest(".contact")) {
            event.preventDefault();
            const d = new FormData(f);
            s.guestEmail = d.get("guestEmail");
            this.requestDisplay();
        }

        if (f.closest('.address [data-name="billingAddress"]')) {
            event.preventDefault();
            s.billingAddress = Object.fromEntries(new FormData(f));
            this.requestDisplay();
        }

        if (f.closest('.address [data-name="shippingAddress"]')) {
            event.preventDefault();
            s.shippingAddress = Object.fromEntries(new FormData(f));
            this.requestDisplay();
        }
    }
}
