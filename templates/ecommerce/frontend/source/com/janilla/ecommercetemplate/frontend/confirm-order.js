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

export default class ConfirmOrder extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["confirm-order"];
    }

    static get observedAttributes() {
        return ["data-guest-email", "data-payment-intent"];
    }

    async updateDisplay() {
        this.appendChild(this.interpolateDom({
            $template: ""
        }));
        const a = this.closest("app-element");
        const j = await (await fetch(`${a.dataset.apiUrl}/payments/stripe/confirm-order`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
                guestEmail: this.dataset.guestEmail,
                paymentIntent: this.dataset.paymentIntent
            })
        })).json();
        if (j?.order) {
            //await fetch(`${a.dataset.apiUrl}/carts/${c.customState.cart.id}`, { method: "DELETE" });
            //localStorage.removeItem("cart");
            const u = new URL(`/orders/${j.order}`, location.href);
            if (this.dataset.guestEmail)
                u.searchParams.append("guestEmail", this.dataset.guestEmail);
            a.navigate(u);
        }
    }
}
