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
import Admin from "admin";
import AdminArray from "admin-array";
import AdminBar from "admin-bar";
import AdminCheckbox from "admin-checkbox";
import AdminCreateFirstUser from "admin-create-first-user";
import AdminDashboard from "admin-dashboard";
import AdminDocument from "admin-document";
import AdminDrawer from "admin-drawer";
import AdminDrawerLink from "admin-drawer-link";
import AdminEdit from "admin-edit";
import AdminFields from "admin-fields";
import AdminFile from "admin-file";
import AdminForgotPassword from "admin-forgot-password";
import AdminHidden from "admin-hidden";
import AdminJoin from "admin-join";
import AdminList from "admin-list";
import AdminLogin from "admin-login";
import AdminPageControls from "admin-page-controls";
import AdminPagination from "admin-pagination";
import AdminPerPage from "admin-per-page";
import AdminRadioGroup from "admin-radio-group";
import AdminRelationship from "admin-relationship";
import AdminRichText from "admin-rich-text";
import AdminSearchBar from "admin-search-bar";
import AdminSearchFilter from "admin-search-filter";
import AdminSelect from "admin-select";
import AdminSlug from "admin-slug";
import AdminTabs from "admin-tabs";
import AdminText from "admin-text";
import AdminUnauthorized from "admin-unauthorized";
import AdminUpload from "admin-upload";
import AdminVersion from "admin-version";
import AdminVersions from "admin-versions";
import App from "app";
import JanillaLogo from "base/janilla-logo";
import LucideIcon from "lucide-icon";
import NotFound from "not-found";
import Page from "page";
import Toaster from "base/toaster";

customElements.define("admin-array", AdminArray);
customElements.define("admin-bar", AdminBar);
customElements.define("admin-checkbox", AdminCheckbox);
customElements.define("admin-create-first-user", AdminCreateFirstUser);
customElements.define("admin-dashboard", AdminDashboard);
customElements.define("admin-document", AdminDocument);
customElements.define("admin-drawer", AdminDrawer);
customElements.define("admin-drawer-link", AdminDrawerLink);
customElements.define("admin-edit", AdminEdit);
customElements.define("admin-element", Admin);
customElements.define("admin-fields", AdminFields);
customElements.define("admin-file", AdminFile);
customElements.define("admin-forgot-password", AdminForgotPassword);
customElements.define("admin-hidden", AdminHidden);
customElements.define("admin-join", AdminJoin);
customElements.define("admin-list", AdminList);
customElements.define("admin-login", AdminLogin);
customElements.define("admin-page-controls", AdminPageControls);
customElements.define("admin-pagination", AdminPagination);
customElements.define("admin-per-page", AdminPerPage);
customElements.define("admin-radio-group", AdminRadioGroup);
customElements.define("admin-relationship", AdminRelationship);
customElements.define("admin-rich-text", AdminRichText);
customElements.define("admin-search-bar", AdminSearchBar);
customElements.define("admin-search-filter", AdminSearchFilter);
customElements.define("admin-select", AdminSelect);
customElements.define("admin-slug", AdminSlug);
customElements.define("admin-tabs", AdminTabs);
customElements.define("admin-text", AdminText);
customElements.define("admin-unauthorized", AdminUnauthorized);
customElements.define("admin-upload", AdminUpload);
customElements.define("admin-version", AdminVersion);
customElements.define("admin-versions", AdminVersions);
customElements.define("app-element", App);
customElements.define("janilla-logo", JanillaLogo);
customElements.define("lucide-icon", LucideIcon);
customElements.define("not-found", NotFound);
customElements.define("page-element", Page);
customElements.define("toaster-element", Toaster);
