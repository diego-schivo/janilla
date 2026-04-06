/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
import AcmeLogo from "acme-logo";
import App from "app";
import BreadcrumbNav from "breadcrumb-nav";
import CardWrapper from "card-wrapper";
import CustomersPage from "customers-page";
import DashboardPage from "dashboard-page";
import DashboardLayout from "dashboard-layout";
import DashboardNav from "dashboard-nav";
import HeroIcon from "hero-icon";
import IntlFormat from "base/intl-format";
import InvoicePage from "invoice-page";
import InvoiceStatus from "invoice-status";
import InvoicesLayout from "invoices-layout";
import InvoicesPage from "invoices-page";
import LatestInvoices from "latest-invoices";
import LoginPage from "login-page";
import PaginationNav from "pagination-nav";
import RevenueChart from "revenue-chart";
import SingleCard from "single-card";
import WelcomePage from "welcome-page";

customElements.define("acme-logo", AcmeLogo);
customElements.define("app-element", App);
customElements.define("breadcrumb-nav", BreadcrumbNav);
customElements.define("card-wrapper", CardWrapper);
customElements.define("customers-page", CustomersPage);
customElements.define("dashboard-page", DashboardPage);
customElements.define("dashboard-layout", DashboardLayout);
customElements.define("dashboard-nav", DashboardNav);
customElements.define("hero-icon", HeroIcon);
customElements.define("intl-format", IntlFormat);
customElements.define("invoice-page", InvoicePage);
customElements.define("invoice-status", InvoiceStatus);
customElements.define("invoices-layout", InvoicesLayout);
customElements.define("invoices-page", InvoicesPage);
customElements.define("latest-invoices", LatestInvoices);
customElements.define("login-page", LoginPage);
customElements.define("pagination-nav", PaginationNav);
customElements.define("revenue-chart", RevenueChart);
customElements.define("single-card", SingleCard);
customElements.define("welcome-page", WelcomePage);
