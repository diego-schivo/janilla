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
import Admin from "cms/admin";
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
import App from "app";
import Article from "article";
import Articles from "articles";
import ArticlePreview from "article-preview";
import Comments from "comments";
import Editor from "editor";
import Errors from "errors";
import FavoriteButton from "favorite-button";
import FollowButton from "follow-button";
import Home from "home";
import IntlFormat from "base/intl-format";
import JanillaLogo from "base/janilla-logo";
import Login from "login";
import LucideIcon from "base/lucide-icon";
import NavLink from "nav-link";
import PageDisplay from "page-display";
import PaginationNav from "pagination-nav";
import PopularTags from "popular-tags";
import Profile from "profile";
import Register from "register";
import Settings from "settings";
import TagsInput from "tags-input";
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
customElements.define("article-element", Article);
customElements.define("articles-element", Articles);
customElements.define("article-preview", ArticlePreview);
customElements.define("comments-element", Comments);
customElements.define("editor-element", Editor);
customElements.define("errors-element", Errors);
customElements.define("favorite-button", FavoriteButton);
customElements.define("follow-button", FollowButton);
customElements.define("home-element", Home);
customElements.define("intl-format", IntlFormat);
customElements.define("janilla-logo", JanillaLogo);
customElements.define("login-element", Login);
customElements.define("lucide-icon", LucideIcon);
customElements.define("nav-link", NavLink);
customElements.define("page-display", PageDisplay);
customElements.define("pagination-nav", PaginationNav);
customElements.define("popular-tags", PopularTags);
customElements.define("profile-element", Profile);
customElements.define("register-element", Register);
customElements.define("settings-element", Settings);
customElements.define("tags-input", TagsInput);
customElements.define("toaster-element", Toaster);
