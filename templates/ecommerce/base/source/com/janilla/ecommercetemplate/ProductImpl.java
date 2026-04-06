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
package com.janilla.ecommercetemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.janilla.cms.DocumentStatus;
import com.janilla.websitetemplate.Category;
import com.janilla.websitetemplate.Meta;

record ProductImpl(Long id, String title, String description, List<GalleryItem> gallery, List<?> layout,
		Boolean enableVariants, List<VariantType> variantTypes, List<Variant> variants, Boolean priceInUsdEnabled,
		BigDecimal priceInUsd, List<Category> categories, Meta meta, String slug, Instant createdAt, Instant updatedAt,
		DocumentStatus documentStatus, Instant publishedAt) implements Product {

	@Override
	public Product withVariants(List<Variant> variants) {
		return new ProductImpl(id, title, description, gallery, layout, enableVariants, variantTypes, variants,
				priceInUsdEnabled, priceInUsd, categories, meta, slug, createdAt, updatedAt, documentStatus,
				publishedAt);
	}
}
