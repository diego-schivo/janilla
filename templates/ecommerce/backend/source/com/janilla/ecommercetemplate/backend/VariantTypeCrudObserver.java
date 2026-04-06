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
import com.janilla.ecommercetemplate.VariantOption;
import com.janilla.ecommercetemplate.VariantType;

public class VariantTypeCrudObserver implements CrudObserver<VariantType> {

	protected final Persistence persistence;

	public VariantTypeCrudObserver(Persistence persistence) {
		this.persistence = persistence;
	}

//	@Override
//	public VariantType afterRead(VariantType entity) {
//		var xx = persistence.crud(VariantOption.class).filter("type", new Object[] { entity.id() });
//		return entity.withOptions(xx.stream().map(x -> VariantOption.EMPTY.withId(x)).toList());
//	}

	@Override
	public VariantType beforeCreate(VariantType entity) {
		if (entity.options() != null)
			entity = entity.withOptions(null);
		return entity;
	}

	@Override
	public VariantType beforeUpdate(VariantType entity) {
		return beforeCreate(entity);
	}

	@Override
	public void afterUpdate(VariantType entity1, VariantType entity2) {
		var xx = persistence.crud(VariantOption.class).filter("type", new Object[] { entity1.id() });
		if (entity2.options() != null)
			xx = xx.stream().filter(x -> entity2.options().stream().allMatch(y -> !y.id().equals(x))).toList();
		persistence.crud(VariantOption.class).delete(xx);
	}

	@Override
	public void afterDelete(VariantType entity) {
		var xx = persistence.crud(VariantOption.class).filter("type", new Object[] { entity.id() });
		persistence.crud(VariantOption.class).delete(xx);
	}
}
