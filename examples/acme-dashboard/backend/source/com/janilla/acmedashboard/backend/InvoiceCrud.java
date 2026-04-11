/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
package com.janilla.acmedashboard.backend;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.persistence.DefaultCrud;
import com.janilla.backend.persistence.Persistence;

class InvoiceCrud extends DefaultCrud<UUID, Invoice> {

	public InvoiceCrud(Persistence persistence) {
		super(Invoice.class, persistence.idConverter(Invoice.class), persistence);
	}

	public BigDecimal getAmount(InvoiceStatus status) {
		return persistence.database().perform(() -> {
			var i = persistence.database().index("StatusAmount", "table");
			var a = new BigDecimal[1];
			i.select(new Object[] { toDatabaseValue(status) }, false,
					x -> x.forEach(y -> a[0] = BigDecimal.valueOf((double) y.reduce((_, z) -> z).get())));
			return Objects.requireNonNullElse(a[0], BigDecimal.ZERO);
		}, false);
	}

	public BigDecimal getAmount(UUID customerId, InvoiceStatus status) {
		return persistence.database().perform(() -> {
			var i = persistence.database().index("CustomerStatusAmount", "table");
			var a = new BigDecimal[1];
			i.select(new Object[] { toDatabaseValue(customerId), toDatabaseValue(status) }, false,
					x -> x.forEach(y -> a[0] = BigDecimal.valueOf((double) y.reduce((_, z) -> z).get())));
			return Objects.requireNonNullElse(a[0], BigDecimal.ZERO);
		}, false);
	}

	@Override
	protected void updateIndexes(Invoice entity1, Invoice entity2) {
		super.updateIndexes(entity1, entity2);

		{
			var dd = Stream.of(entity1, entity2).filter(Objects::nonNull)
					.collect(Collectors.toMap(x -> x.status(), _ -> BigDecimal.ZERO, (x, _) -> x, LinkedHashMap::new));
			if (entity1 != null)
				dd.compute(entity1.status(), (_, x) -> x.subtract(entity1.amount()));
			if (entity2 != null)
				dd.compute(entity2.status(), (_, x) -> x.add(entity2.amount()));
			var i = persistence.database().index("StatusAmount", "table");
			dd.entrySet().forEach(sd -> {
				var s = sd.getKey();
				var d = sd.getValue();
				if (d.compareTo(BigDecimal.ZERO) != 0) {
					var k = new Object[] { toDatabaseValue(s) };
					var a = new BigDecimal[1];
					i.delete(k, x -> x.forEach(y -> a[0] = BigDecimal.valueOf((double) y.reduce((_, z) -> z).get())));
					a[0] = a[0] != null ? a[0].add(d) : d;
					i.insert(k, new Object[] { toDatabaseValue(a[0]) });
				}
			});
		}

		{
			var dd = Stream.of(entity1, entity2).filter(Objects::nonNull)
					.collect(Collectors.toMap(x -> List.of(x.customer().id(), x.status()), _ -> BigDecimal.ZERO,
							(x, _) -> x, LinkedHashMap::new));
			if (entity1 != null)
				dd.compute(List.of(entity1.customer().id(), entity1.status()), (_, x) -> x.subtract(entity1.amount()));
			if (entity2 != null)
				dd.compute(List.of(entity2.customer().id(), entity2.status()), (_, x) -> x.add(entity2.amount()));
			var i = persistence.database().index("CustomerStatusAmount", "table");
			dd.entrySet().forEach(csd -> {
				var c = csd.getKey().getFirst();
				var s = csd.getKey().get(1);
				var d = csd.getValue();
				if (d.compareTo(BigDecimal.ZERO) != 0) {
					var k = new Object[] { toDatabaseValue(c), toDatabaseValue(s) };
					var a = new BigDecimal[1];
					i.delete(k, x -> x.forEach(y -> a[0] = BigDecimal.valueOf((double) y.reduce((_, z) -> z).get())));
					a[0] = a[0] != null ? a[0].add(d) : d;
					i.insert(k, new Object[] { toDatabaseValue(a[0]) });
				}
			});
		}
	}
}
