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
import CmsAdmin from "cms/admin";

export default class Admin extends CmsAdmin {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["/cms/admin"];
    }

    static get observedAttributes() {
        return ["data-user", "data-uri"];
    }

    headers(entitySlug) {
        switch (entitySlug) {
            case "owners":
            case "vets":
                return ["firstName", "lastName"];
            case "pet-types":
            case "pets":
            case "specialties":
                return ["name"];
            case "visits":
                return ["date"];
            default:
                return super.headers(entitySlug);
        }
    }

    title(document) {
        switch (document.$type) {
            case "Owner":
            case "Vet":
                return `${document.firstName} ${document.lastName}`;
            case "Pet":
            case "PetType":
            case "Specialty":
                return document.name;
            case "Visit":
                return document.date;
            default:
                return super.title(document);
        }
    }
}
