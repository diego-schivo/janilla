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

export default class Payment extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["payment"];
    }

    static get observedAttributes() {
        return ["data-guest-email", "data-amount"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        this.appendChild(this.interpolateDom({
            $template: "",
            submit: {
                $template: "submit",
				disabled: s.submitting,
                text: s.submitting ? "Loading..." : "Pay now"
            }
        }));

        if (!s.elements) {
            const a = this.closest("app-element");
            s.elements = a.customState.stripe.elements({
                appearance: {
                    theme: "stripe",
                    variables: {
                        borderRadius: "6px",
                        colorPrimary: "#858585",
                        gridColumnSpacing: "20px",
                        gridRowSpacing: "20px",
                        colorBackground: { light: "rgb(255, 255, 255)", dark: "#0a0a0a" }[a.colorScheme],
                        colorDanger: "rgb(255, 111, 118)",
                        colorDangerText: "rgb(255, 111, 118)",
                        colorIcon: { light: "rgb(0, 0, 0)", dark: "rgb(255, 255, 255)" }[a.colorScheme],
                        colorText: { light: "rgb(0, 0, 0)", dark: "#858585" }[a.colorScheme],
                        colorTextPlaceholder: "#858585",
                        fontFamily: "Geist, sans-serif",
                        fontSizeBase: "16px",
                        fontWeightBold: "600",
                        fontWeightNormal: "500",
                        spacingUnit: "4px"
                    }
                },
                clientSecret: this.closest("checkout-element").customState.paymentData.clientSecret
            });
            s.elements.create("payment").mount("#payment-element");
        }
    }

    handleClick = event => {
        const el = event.target.closest("button");
        if (el?.name === "cancel")
            this.closest("checkout-element").paymentData = null;
    }

    handleSubmit = async event => {
        event.preventDefault();

        const s = this.customState;
        s.submitting = true;
		this.requestDisplay();

		        const c = this.closest("checkout-element");
        const a = this.closest("app-element");
        const [ba] = [c.customState.billingAddress].map(x => typeof x === "object" ? x : a.currentUser.addresses.find(y => y.id === x));
        let u = new URL("/checkout/confirm-order", location.href);
        if (this.dataset.guestEmail)
            u.searchParams.append("guest_email", this.dataset.guestEmail);
        let j = await a.customState.stripe.confirmPayment({
            confirmParams: {
                return_url: u.toString(),
                payment_method_data: {
                    billing_details: {
                        email: this.dataset.guestEmail,
                        phone: ba.phone,
                        address: {
                            line1: ba.addressLine1,
                            line2: ba.addressLine2,
                            city: ba.city,
                            state: ba.customState,
                            postal_code: ba.postalCode,
                            country: ba.country,
                        },
                    },
                }
            },
            elements: s.elements,
            redirect: "if_required"
        });
        if (j.paymentIntent?.status === "succeeded") {
            j = await (await fetch(`${a.dataset.apiUrl}/payments/stripe/confirm-order`, {
                method: "POST",
                headers: { "content-type": "application/json" },
                body: JSON.stringify({
                    guestEmail: this.dataset.guestEmail,
                    paymentIntent: j.paymentIntent.id
                })
            })).json();
            if (j?.order) {
                await fetch(`${a.dataset.apiUrl}/carts/${c.customState.cart.id}`, { method: "DELETE" });
                localStorage.removeItem("cart");
                u = new URL(`/orders/${j.order}`, location.href);
                if (this.dataset.guestEmail)
                    u.searchParams.append("guestEmail", this.dataset.guestEmail);
                a.navigate(u);
            }
        }
    }
}
