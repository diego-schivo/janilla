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
import java.util.List;

import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.cms.Versions;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;
import com.janilla.websitetemplate.CallToAction;
import com.janilla.websitetemplate.Category;
import com.janilla.websitetemplate.Content;
import com.janilla.websitetemplate.MediaBlock;
import com.janilla.websitetemplate.Meta;

@Store
@Versions(drafts = true)
public interface Product extends Document<Long> {

	@Index
	String title();

	String description();

	List<GalleryItem> gallery();

	List<@Types({ CallToAction.class, Content.class, MediaBlock.class }) ?> layout();

	Boolean enableVariants();

	List<VariantType> variantTypes();

	List<Variant> variants();

	Boolean priceInUsdEnabled();

	BigDecimal priceInUsd();

	@Index
	List<Category> categories();

	Meta meta();

	@Index
	String slug();

	Product withVariants(List<Variant> variants);
}
