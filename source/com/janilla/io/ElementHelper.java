/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.io;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import com.janilla.reflect.Reflection;

public interface ElementHelper<E> {

	byte[] getBytes(E element);

	int getLength(ByteBuffer buffer);

	E getElement(ByteBuffer buffer);

	int compare(ByteBuffer buffer, E element);

	static <T> ElementHelper<T> of(Class<T> type) {
		if (type == BigDecimal.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) BIG_DECIMAL;
			return h;
		} else if (type == Instant.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) INSTANT;
			return h;
		} else if (type == Integer.class || type == Integer.TYPE) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) INTEGER;
			return h;
		} else if (type == LocalDate.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) LOCAL_DATE;
			return h;
		} else if (type == Long.class || type == Long.TYPE) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) LONG;
			return h;
		} else if (type == String.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) STRING;
			return h;
		} else if (type == URI.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) URI1;
			return h;
		} else if (type == UUID.class) {
			@SuppressWarnings("unchecked")
			var h = (ElementHelper<T>) UUID1;
			return h;
		} else
			throw new IllegalArgumentException("type=" + type);
	}

	static ElementHelper<Object[]> of(Class<?> type, String... properties) {
		return of(Arrays.stream(properties).map(n -> {
			var d = n.startsWith("-");
			if (n.startsWith("+") || d)
				n = n.substring(1);
			var t = Reflection.property(type, n).getType();
			var o = d ? SortOrder.DESCENDING : SortOrder.ASCENDING;
			return new TypeAndOrder(t, o);
		}).toArray(TypeAndOrder[]::new));
	}

	static ElementHelper<Object[]> of(TypeAndOrder... types) {
		var h = Arrays.stream(types).map(x -> ElementHelper.of(x.type)).toArray(ElementHelper[]::new);
		var o = Arrays.stream(types).map(TypeAndOrder::order).toArray(SortOrder[]::new);
		return new ElementHelper<>() {

			@Override
			public byte[] getBytes(Object[] element) {
				var b = new byte[h.length][];
				var l = 0;
				for (var i = 0; i < h.length; i++) {
					@SuppressWarnings("unchecked")
					var c = h[i].getBytes(element[i]);
					b[i] = c;
					l += c.length;
				}
				var c = ByteBuffer.allocate(l);
				for (var d : b)
					c.put(d);
				return c.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				var p = buffer.position();
				var q = p;
				try {
					for (var i : h) {
						var l = i.getLength(buffer);
						q += l;
						if (l < 0 || q > buffer.limit()) {
							System.out.println("l=" + l);
							throw new RuntimeException();
						}
						buffer.position(q);
					}
					return q - p;
				} finally {
					buffer.position(p);
				}
			}

			@Override
			public Object[] getElement(ByteBuffer buffer) {
				return Arrays.stream(h).map(h -> h.getElement(buffer)).toArray();
			}

			@Override
			public int compare(ByteBuffer buffer, Object[] element) {
				var p = buffer.position();
				try {
					for (var i = 0; i < h.length; i++) {
						@SuppressWarnings("unchecked")
						var c = h[i].compare(buffer, element[i]);
						if (c != 0)
							return o[i] == SortOrder.DESCENDING ? -c : c;
						buffer.position(buffer.position() + h[i].getLength(buffer));
					}
					return 0;
				} finally {
					buffer.position(p);
				}
			}
		};
	}

	ElementHelper<BigDecimal> BIG_DECIMAL = new ElementHelper<>() {

		@Override
		public byte[] getBytes(BigDecimal element) {
			var b = ByteBuffer.allocate(8 + 2);
			b.putLong(element.unscaledValue().longValue());
			b.putShort((short) element.scale());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 8 + 2;
		}

		@Override
		public BigDecimal getElement(ByteBuffer buffer) {
			return BigDecimal.valueOf(buffer.getLong(), buffer.getShort());
		}

		@Override
		public int compare(ByteBuffer buffer, BigDecimal element) {
			var d = BigDecimal.valueOf(buffer.getLong(buffer.position()), buffer.getShort(buffer.position() + 8));
			return d.compareTo(element);
		}
	};

	ElementHelper<Boolean> BOOLEAN = new ElementHelper<>() {

		@Override
		public byte[] getBytes(Boolean element) {
			var b = ByteBuffer.allocate(1);
			b.put(element ? (byte) 1 : 0);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 1;
		}

		@Override
		public Boolean getElement(ByteBuffer buffer) {
			return buffer.get() != (byte) 0;
		}

		@Override
		public int compare(ByteBuffer buffer, Boolean element) {
			return Boolean.compare(buffer.get(buffer.position()) != (byte) 0, element);
		}
	};

	ElementHelper<Instant> INSTANT = new ElementHelper<>() {

		@Override
		public byte[] getBytes(Instant element) {
			var b = ByteBuffer.allocate(8);
			b.putLong(element.toEpochMilli());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 8;
		}

		@Override
		public Instant getElement(ByteBuffer buffer) {
			return Instant.ofEpochMilli(buffer.getLong());
		}

		@Override
		public int compare(ByteBuffer buffer, Instant element) {
			return Long.compare(buffer.getLong(buffer.position()), element.toEpochMilli());
		}
	};

	ElementHelper<Integer> INTEGER = new ElementHelper<>() {

		@Override
		public byte[] getBytes(Integer element) {
			var b = ByteBuffer.allocate(4);
			b.putInt(element);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 4;
		}

		@Override
		public Integer getElement(ByteBuffer buffer) {
			return buffer.getInt();
		}

		@Override
		public int compare(ByteBuffer buffer, Integer element) {
			return Integer.compare(buffer.getInt(buffer.position()), element);
		}
	};

	ElementHelper<LocalDate> LOCAL_DATE = new ElementHelper<>() {

		@Override
		public byte[] getBytes(LocalDate element) {
			var b = ByteBuffer.allocate(4 + 2 * 1);
			b.putInt(element.getYear());
			b.put((byte) element.getMonthValue());
			b.put((byte) element.getDayOfMonth());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 4 + 2 * 1;
		}

		@Override
		public LocalDate getElement(ByteBuffer buffer) {
			return LocalDate.of(buffer.getInt(), buffer.get(), buffer.get());
		}

		@Override
		public int compare(ByteBuffer buffer, LocalDate element) {
			var c = Integer.compare(buffer.getInt(buffer.position()), element.getYear());
			if (c != 0)
				return c;
			c = Byte.compare(buffer.get(buffer.position() + 4), (byte) element.getMonthValue());
			return c != 0 ? c : Byte.compare(buffer.get(buffer.position() + 4 + 1), (byte) element.getDayOfMonth());
		}
	};

	ElementHelper<Long> LONG = new ElementHelper<>() {

		@Override
		public byte[] getBytes(Long element) {
			var b = ByteBuffer.allocate(8);
			b.putLong(element);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 8;
		}

		@Override
		public Long getElement(ByteBuffer buffer) {
			return buffer.getLong();
		}

		@Override
		public int compare(ByteBuffer buffer, Long element) {
			return Long.compare(buffer.getLong(buffer.position()), element);
		}
	};

	ElementHelper<Object> NULL = new ElementHelper<>() {

		@Override
		public byte[] getBytes(Object element) {
			return new byte[0];
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 0;
		}

		@Override
		public Object getElement(ByteBuffer buffer) {
			return null;
		}

		@Override
		public int compare(ByteBuffer buffer, Object element) {
			return 0;
		}
	};

	ElementHelper<String> STRING = new ElementHelper<>() {

		@Override
		public byte[] getBytes(String element) {
			var b = element.getBytes();
			var c = ByteBuffer.allocate(4 + b.length);
			c.putInt(b.length);
			c.put(b);
			return c.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 4 + buffer.getInt(buffer.position());
		}

		@Override
		public String getElement(ByteBuffer buffer) {
			var b = new byte[buffer.getInt()];
			buffer.get(b);
			return new String(b);
		}

		@Override
		public int compare(ByteBuffer buffer, String element) {
			var p = buffer.position();
			var l = buffer.getInt(p);
			if (l < 0 || p + 4 + l > buffer.limit()) {
				System.out.println("l=" + l);
				throw new RuntimeException();
			}
			var b = new byte[l];
			buffer.get(p + 4, b);
			if (element == null)
				throw new RuntimeException();
			return new String(b).compareTo(element);
		}
	};

	ElementHelper<URI> URI1 = new ElementHelper<>() {

		@Override
		public byte[] getBytes(URI element) {
			return STRING.getBytes(element.toString());
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return STRING.getLength(buffer);
		}

		@Override
		public URI getElement(ByteBuffer buffer) {
			return URI.create(STRING.getElement(buffer));
		}

		@Override
		public int compare(ByteBuffer buffer, URI element) {
			return STRING.compare(buffer, element.toString());
		}
	};

	ElementHelper<UUID> UUID1 = new ElementHelper<>() {

		@Override
		public byte[] getBytes(UUID element) {
			var b = ByteBuffer.allocate(16);
			b.putLong(element.getMostSignificantBits());
			b.putLong(element.getLeastSignificantBits());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 16;
		}

		@Override
		public UUID getElement(ByteBuffer buffer) {
			return new UUID(buffer.getLong(), buffer.getLong());
		}

		@Override
		public int compare(ByteBuffer buffer, UUID element) {
			var c = Long.compare(buffer.getLong(buffer.position()), element.getMostSignificantBits());
			return c != 0 ? c : Long.compare(buffer.getLong(buffer.position() + 8), element.getLeastSignificantBits());
		}
	};

	public record TypeAndOrder(Class<?> type, SortOrder order) {
	}

	public enum SortOrder {

		ASCENDING, DESCENDING
	}
}
