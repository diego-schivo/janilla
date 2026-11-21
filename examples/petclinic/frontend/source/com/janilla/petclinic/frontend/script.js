/*
 * Copyright 2012-2025 the original author or authors.
 * Copyright 2024-2026 Diego Schivo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Admin from "admin";
import AdminArray from "cms/admin-array";
import AdminBar from "cms/admin-bar";
import AdminCheckbox from "cms/admin-checkbox";
import AdminCreateFirstUser from "cms/admin-create-first-user";
import AdminDashboard from "cms/admin-dashboard";
import AdminDateTime from "cms/admin-date-time";
import AdminDocument from "cms/admin-document";
import AdminDrawer from "cms/admin-drawer";
import AdminDrawerLink from "cms/admin-drawer-link";
import AdminEdit from "cms/admin-edit";
import AdminFields from "cms/admin-fields";
import AdminFile from "cms/admin-file";
import AdminForgotPassword from "cms/admin-forgot-password";
import AdminHidden from "cms/admin-hidden";
import AdminJoin from "cms/admin-join";
import AdminList from "cms/admin-list";
import AdminLogin from "cms/admin-login";
import AdminNavToggler from "cms/admin-nav-toggler";
import AdminPageControls from "cms/admin-page-controls";
import AdminPagination from "cms/admin-pagination";
import AdminPassword from "cms/admin-password";
import AdminPerPage from "cms/admin-per-page";
import AdminRadioGroup from "cms/admin-radio-group";
import AdminRelationship from "cms/admin-relationship";
import AdminRichText from "cms/admin-rich-text";
import AdminSearchBar from "cms/admin-search-bar";
import AdminSearchFilter from "cms/admin-search-filter";
import AdminSelect from "cms/admin-select";
import AdminSlug from "cms/admin-slug";
import AdminTabs from "cms/admin-tabs";
import AdminText from "cms/admin-text";
import AdminUnauthorized from "cms/admin-unauthorized";
import AdminUpload from "cms/admin-upload";
import AdminVersion from "cms/admin-version";
import AdminVersions from "cms/admin-versions";
import App from "blank/app";
import JanillaLogo from "base/janilla-logo";
import LucideIcon from "base/lucide-icon";
import NotFound from "blank/not-found";
import Page from "blank/page";
import Toaster from "base/toaster";

customElements.define("admin-array", AdminArray);
customElements.define("admin-bar", AdminBar);
customElements.define("admin-checkbox", AdminCheckbox);
customElements.define("admin-create-first-user", AdminCreateFirstUser);
customElements.define("admin-dashboard", AdminDashboard);
customElements.define("admin-date-time", AdminDateTime);
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
customElements.define("admin-nav-toggler", AdminNavToggler);
customElements.define("admin-page-controls", AdminPageControls);
customElements.define("admin-pagination", AdminPagination);
customElements.define("admin-password", AdminPassword);
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
