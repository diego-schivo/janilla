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
import WebsiteApp from "website/app";

const ordersRegex = /^\/orders(\/.*)?$/;
const productRegex = /^\/products(\/.*)$/;

export default class App extends WebsiteApp {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
		return ["/base/app", "/blank/app", "/website/app", "app"];
    }

    async updateDisplaySite() {
        const s = this.customState;

        const u0 = this.currentUser;
        const p = location.pathname;

        if (u0)
            switch (p) {
                case "/create-account":
                case "/login":
                    const u = new URL("/account", location.href);
                    u.searchParams.append("warning", "You are already logged in.");
                    this.navigate(u);
                    return;
                case "/logout":
                    await fetch(`${this.dataset.apiUrl}/users/logout`, { method: "POST" });
                    s.user = null;
                    break;
            }
        else if (p === "/account" || p.startsWith("/account/")) {
            const u = new URL("/login", location.href);
            u.searchParams.append("warning", "Please login to access your account settings.");
            this.navigate(u);
            return;
        } else if (p === "/orders" || p.startsWith("/orders/"))
            s.notFound = true;

        await super.updateDisplaySite();
    }

    siteData() {
        const x = super.siteData();
        x.after = { $template: "toaster" };
        return x;
    }

    contentData() {
        const s = this.customState;
        if (!s.notFound) {
            const p = location.pathname;
            const spp = new URLSearchParams(location.search);
            switch (p) {
                case "/account":
                    return {
                        $template: "account",
                        success: spp.get("success"),
                        warning: spp.get("warning")
                    };
                case "/account/addresses":
                    return { $template: "addresses" };
                case "/checkout":
                    if (!Array.from(document.head.querySelectorAll("script"))
                        .some(x => x.src === this.dataset.stripeUrl)) {
                        const el = document.createElement("script");
                        el.onload = () => {
                            if (!s.stripe && typeof Stripe !== "undefined")
                                s.stripe = Stripe(this.dataset.stripePublishableKey);
                        }
                        document.head.append(el);
                        el.src = this.dataset.stripeUrl;
                    } else if (!s.stripe && typeof Stripe !== "undefined")
                        s.stripe = Stripe(this.dataset.stripePublishableKey);
                    return { $template: "checkout" };
                case "/checkout/confirm-order":
                    return {
                        $template: "confirm-order",
                        guestEmail: spp.get("guest_email"),
                        paymentIntent: spp.get("payment_intent")
                    };
                case "/create-account":
                    return { $template: "create-account" };
                case "/find-order":
                    return { $template: "find-order" };
                case "/login":
                    return {
                        $template: "login",
                        warning: spp.get("warning")
                    };
                case "/logout":
                    return {
                        $template: "logout",
                        noOp: !u0
                    };
            }
            {
                const m = p.match(productRegex);
                if (m)
                    return {
                        $template: "product",
                        slug: m[1].substring(1),
                        search: location.search
                    };
            }
            {
                const m = p.match(ordersRegex);
                if (m)
                    return m[1] ? {
                        $template: "order",
                        id: m[1].substring(1)
                    } : { $template: "orders" };
            }
            if (p === "/shop")
                return {
                    $template: "shop",
                    query: spp.get("q"),
                    category: spp.get("category"),
                    sort: spp.get("sort")
                };
        }
        return super.contentData();
    }

    success() {
        const el = this.querySelector("toaster-element");
        el.success.apply(el, arguments);
    }

    error() {
        const el = this.querySelector("toaster-element");
        el.error.apply(el, arguments);
    }

    async enumValues(name) {
        const s = this.customState;
        if (!Object.hasOwn(s, "enums"))
            s.enums = await (await fetch(`${this.dataset.apiUrl}/enums`)).json();
        return s.enums[name];
    }
}
