/*
 * MIT License
 *
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
import Admin from "website/admin";
import AdminArray from "cms/admin-array";
import AdminBar from "website/admin-bar";
import AdminCheckbox from "cms/admin-checkbox";
import AdminCreateFirstUser from "cms/admin-create-first-user";
import AdminDashboard from "website/admin-dashboard";
import AdminDocument from "cms/admin-document";
import AdminDrawer from "cms/admin-drawer";
import AdminDrawerLink from "cms/admin-drawer-link";
import AdminEdit from "cms/admin-edit";
import AdminFields from "cms/admin-fields";
import AdminFile from "cms/admin-file";
import AdminHidden from "cms/admin-hidden";
import AdminJoin from "cms/admin-join";
import AdminList from "cms/admin-list";
import AdminLogin from "cms/admin-login";
import AdminRadioGroup from "cms/admin-radio-group";
import AdminRelationship from "cms/admin-relationship";
import AdminRichText from "cms/admin-rich-text";
import AdminSelect from "cms/admin-select";
import AdminSlug from "cms/admin-slug";
import AdminTabs from "cms/admin-tabs";
import AdminText from "cms/admin-text";
import AdminUnauthorized from "cms/admin-unauthorized";
import AdminUpload from "cms/admin-upload";
import AdminVersion from "cms/admin-version";
import AdminVersions from "cms/admin-versions";
import App from "website/app";
import Archive from "website/archive";
import Banner from "website/banner";
import CallToAction from "website/call-to-action";
import Card from "website/card";
import Content from "website/content";
import Footer from "website/footer";
import FormBlock from "website/form-block";
import Header from "header";
import Hero from "website/hero";
import JanillaLogo from "base/janilla-logo";
import Link from "link";
import LucideIcon from "blank/lucide-icon";
import MediaBlock from "website/media-block";
import NotFound from "blank/not-found";
import Page from "website/page";
import Post from "post";
import Posts from "website/posts";
import RichText from "website/rich-text";
import Search from "website/search";
import ThemeSelector from "website/theme-selector";
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
customElements.define("admin-hidden", AdminHidden);
customElements.define("admin-join", AdminJoin);
customElements.define("admin-list", AdminList);
customElements.define("admin-login", AdminLogin);
customElements.define("admin-radio-group", AdminRadioGroup);
customElements.define("admin-relationship", AdminRelationship);
customElements.define("admin-rich-text", AdminRichText);
customElements.define("admin-select", AdminSelect);
customElements.define("admin-slug", AdminSlug);
customElements.define("admin-tabs", AdminTabs);
customElements.define("admin-text", AdminText);
customElements.define("admin-unauthorized", AdminUnauthorized);
customElements.define("admin-upload", AdminUpload);
customElements.define("admin-version", AdminVersion);
customElements.define("admin-versions", AdminVersions);
customElements.define("app-element", App);
customElements.define("archive-element", Archive);
customElements.define("banner-element", Banner);
customElements.define("call-to-action", CallToAction);
customElements.define("card-element", Card);
customElements.define("content-element", Content);
customElements.define("footer-element", Footer);
customElements.define("form-block", FormBlock);
customElements.define("header-element", Header);
customElements.define("hero-element", Hero);
customElements.define("janilla-logo", JanillaLogo);
customElements.define("link-element", Link);
customElements.define("lucide-icon", LucideIcon);
customElements.define("media-block", MediaBlock);
customElements.define("not-found", NotFound);
customElements.define("page-element", Page);
customElements.define("post-element", Post);
customElements.define("posts-element", Posts);
customElements.define("rich-text", RichText);
customElements.define("search-element", Search);
customElements.define("theme-selector", ThemeSelector);
customElements.define("toaster-element", Toaster);
