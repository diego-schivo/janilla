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
import WebsiteAdmin from "website/admin";

export default class Admin extends WebsiteAdmin {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["/cms/admin"];
    }

    static get observedAttributes() {
        return ["data-user", "data-uri"];
    }

    cell(object, key) {
        const x = super.cell(object, key);
        switch (key) {
            case "country":
            case "currency":
                return x?.name;
            case "items":
                return `${x.length} items`;
            case "options":
                return x.map(y => y.label).join();
        }
        return x;
    }

    dashboardGroups() {
        return super.dashboardGroups().filter(x => x !== "collections");
    }

    headers(entitySlug) {
        switch (entitySlug) {
            case "addresses":
                return ["id", "addressLine1", "city", "postalCode", "country"];
            case "carts":
                return ["id", "createdAt", "currency", "items"];
            case "categories":
                return ["title", "slug", "updatedAt", "createdAt"];
            case "products":
                return ["title", "enableVariants", "documentStatus"];
            case "variant-options":
                return ["id", "label", "value"];
            case "variant-types":
                return ["id", "label", "name"];
            case "variants":
                return ["id", "title", "options", "inventory", "documentStatus"];
        }
        return super.headers(entitySlug);
    }

    controlTemplate(field) {
        switch (field.type) {
            /*
            case "List":
                if (field.name === "addresses")
                    return "relationship-table";
                break;
            */
            case "List":
                switch (field.name) {
                    case "options":
                        //return "join";
                        return "variant-options";
                    case "variantTypes":
                        return "relationship";
                }
                break;
            /*
            case "Set":
                if (field.name === "options" && field.parent.type === "Variant")
                    return "variant-options";
                break;
            */
            case "String":
                switch (field.name) {
                    case "type":
                        return field.options.length <= 2 ? "radio-group" : "select";
                }
                break;
        }
        return super.controlTemplate(field);
    }

    tabs(type) {
        switch (type) {
            case "Product":
                return {
                    Content: ["description", "gallery", "layout"],
                    ProductDetails: ["enableVariants", "variantTypes", "variants", "stock", "price"],
                    SEO: ["meta"]
                };
        }
        return super.tabs(type);
    }

    preview(entity) {
        switch (entity.$type) {
            case "Product":
                return `/products/${entity.slug}`;
        }
        return super.preview(entity);
    }

    sidebar(type) {
        switch (type) {
            case "Product":
                return ["publishedAt", "slug", "authors"];
        }
        return super.sidebar(type);
    }

    /*
    setValue(object, key, value, field) {
        if (field.name === "options" && field.parent.type === "Variant") {
            const vv = value.split(":");
            value = {
                $type: `${vv[0]}Option`,
                name: vv[1]
            };
        }
        return super.setValue(object, key, value, field);
    }
    */

    title(document) {
        switch (document.$type) {
            case "VariantOption":
            case "VariantType":
                return document.label;
            default:
                return super.title(document);
        }
    }

    fieldLabel(ct) {
        return ct === "variant-options" ? "no-label" : super.fieldLabel(ct);
    }
}
