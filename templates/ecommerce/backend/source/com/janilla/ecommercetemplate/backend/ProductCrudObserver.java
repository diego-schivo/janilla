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
package com.janilla.ecommercetemplate.backend;

import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.persistence.Persistence;
import com.janilla.ecommercetemplate.Product;
import com.janilla.ecommercetemplate.Variant;

public class ProductCrudObserver implements CrudObserver<Product> {

	protected final Persistence persistence;

	public ProductCrudObserver(Persistence persistence) {
		this.persistence = persistence;
	}

//	@Override
//	public Product afterRead(Product entity) {
//		var xx = persistence.crud(Variant.class).filter("product", new Object[] { entity.id() });
//		return entity.withVariants(xx.stream().map(x -> Variant.EMPTY.withId(x)).toList());
//	}

	@Override
	public Product beforeCreate(Product entity) {
		if (entity.variants() != null)
			entity = entity.withVariants(null);
		return entity;
	}

	@Override
	public Product beforeUpdate(Product entity) {
		return beforeCreate(entity);
	}

	@Override
	public void afterUpdate(Product entity1, Product entity2) {
		var xx = persistence.crud(Variant.class).filter("product", new Object[] { entity1.id() });
		if (entity2.variants() != null)
			xx = xx.stream().filter(x -> !entity2.variants().stream().allMatch(y -> !y.id().equals(x))).toList();
		persistence.crud(Variant.class).delete(xx);
	}

	@Override
	public void afterDelete(Product entity) {
		var xx = persistence.crud(Variant.class).filter("product", new Object[] { entity.id() });
		persistence.crud(Variant.class).delete(xx);
	}
}
