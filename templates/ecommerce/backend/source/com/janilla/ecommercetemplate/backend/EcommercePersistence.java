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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.ecommercetemplate.Product;
import com.janilla.ecommercetemplate.VariantType;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Copier;
import com.janilla.java.Property;
import com.janilla.persistence.Entity;
import com.janilla.websitetemplate.backend.WebsitePersistence;

public class EcommercePersistence<C extends EcommerceBackendConfig> extends WebsitePersistence<C> {

	public EcommercePersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
			DiFactory diFactory, C config, Class<?> seedDataClass, Copier copier) {
		super(database, storables, diFactory, config, seedDataClass, copier);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			Class<? extends CrudObserver<?>> t;
			if (type.equals(Cart.class))
				t = CartCrudObserver.class;
			else if (type.equals(Product.class))
				t = ProductCrudObserver.class;
			else if (type.equals(User.class))
				t = UserCrudObserver.class;
			else if (type.equals(VariantType.class))
				t = VariantTypeCrudObserver.class;
			else
				t = null;
			if (t != null) {
				@SuppressWarnings("unchecked")
				var o = (CrudObserver<E>) diFactory.newInstance(t, Map.of("persistence", this));
				c.observers().add(o);
			}
		}
		return c;
	}

	@Override
	protected List<Property> seedProperties() {
		var pp = super.seedProperties();

		var ii = Stream.of("products", "variants", "carts").mapToInt(
				n -> IntStream.range(0, pp.size()).filter(i -> pp.get(i).name().equals(n)).findFirst().orElseThrow())
				.toArray();
		var c = Arrays.stream(ii).mapToObj(pp::get).toList();
		pp.removeAll(c);

		var i = Arrays.stream(ii).min().getAsInt();
		pp.addAll(i, c);

		return pp;
	}
}
