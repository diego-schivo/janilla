/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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

public interface ByteConverter<E> {

	byte[] serialize(E element);

	int getLength(ByteBuffer buffer);

	E deserialize(ByteBuffer buffer);

	int compare(ByteBuffer buffer, E element);

	static <T> ByteConverter<T> of(Class<T> type) {
		if (type == BigDecimal.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) BIG_DECIMAL;
			return h;
		} else if (type == Instant.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) INSTANT;
			return h;
		} else if (type == Integer.class || type == Integer.TYPE) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) INTEGER;
			return h;
		} else if (type == LocalDate.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) LOCAL_DATE;
			return h;
		} else if (type == Long.class || type == Long.TYPE) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) LONG;
			return h;
		} else if (type == String.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) STRING;
			return h;
		} else if (type == URI.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) URI1;
			return h;
		} else if (type == UUID.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<T>) UUID1;
			return h;
		} else
			throw new IllegalArgumentException("type=" + type);
	}

	static ByteConverter<Object[]> of(Class<?> type, String... properties) {
		return of(Arrays.stream(properties).map(n -> {
			var d = n.startsWith("-");
			if (n.startsWith("+") || d)
				n = n.substring(1);
			var t = Reflection.property(type, n).type();
			var o = d ? SortOrder.DESCENDING : SortOrder.ASCENDING;
			return new TypeAndOrder(t, o);
		}).toArray(TypeAndOrder[]::new));
	}

	static ByteConverter<Object[]> of(TypeAndOrder... types) {
		var hh = Arrays.stream(types).map(x -> ByteConverter.of(x.type)).toArray(ByteConverter[]::new);
		var oo = Arrays.stream(types).map(TypeAndOrder::order).toArray(SortOrder[]::new);
		return new ByteConverter<>() {

			@Override
			public byte[] serialize(Object[] element) {
				var bbb = new byte[hh.length][];
				var l = 0;
				for (var i = 0; i < hh.length; i++) {
					@SuppressWarnings("unchecked")
					var bb = hh[i].serialize(element[i]);
					bbb[i] = bb;
					l += bb.length;
				}
				var b = ByteBuffer.allocate(l);
				for (var bb : bbb)
					b.put(bb);
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				var p1 = buffer.position();
				var p2 = p1;
				try {
					for (var h : hh) {
						var l = h.getLength(buffer);
						p2 += l;
						if (l < 0 || p2 > buffer.limit()) {
//							System.out.println("l=" + l);
							throw new RuntimeException();
						}
						buffer.position(p2);
					}
					return p2 - p1;
				} finally {
					buffer.position(p1);
				}
			}

			@Override
			public Object[] deserialize(ByteBuffer buffer) {
				return Arrays.stream(hh).map(h -> h.deserialize(buffer)).toArray();
			}

			@Override
			public int compare(ByteBuffer buffer, Object[] element) {
				var p = buffer.position();
				try {
					for (var i = 0; i < hh.length; i++) {
						@SuppressWarnings("unchecked")
						var c = hh[i].compare(buffer, element[i]);
						if (c != 0)
							return oo[i] == SortOrder.DESCENDING ? -c : c;
						buffer.position(buffer.position() + hh[i].getLength(buffer));
					}
					return 0;
				} finally {
					buffer.position(p);
				}
			}
		};
	}

	ByteConverter<BigDecimal> BIG_DECIMAL = new ByteConverter<>() {

		@Override
		public byte[] serialize(BigDecimal element) {
			var b = ByteBuffer.allocate(Long.BYTES + Short.BYTES);
			b.putLong(element != null ? element.unscaledValue().longValue() : -1);
			b.putShort((short) (element != null ? element.scale() : -1));
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Long.BYTES + Short.BYTES;
		}

		@Override
		public BigDecimal deserialize(ByteBuffer buffer) {
			var l = buffer.getLong();
			var s = buffer.getShort();
			return s != -1 ? BigDecimal.valueOf(l, s) : null;
		}

		@Override
		public int compare(ByteBuffer buffer, BigDecimal element) {
			var l = buffer.getLong(buffer.position());
			var s = buffer.getShort(buffer.position() + Long.BYTES);
			if (s == -1)
				return element == null ? 0 : -1;
			if (element == null)
				return 1;
			return BigDecimal.valueOf(l, s).compareTo(element);
		}
	};

	ByteConverter<Boolean> BOOLEAN = new ByteConverter<>() {

		@Override
		public byte[] serialize(Boolean element) {
			return new byte[] { (byte) (element == null ? -1 : element ? 1 : 0) };
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 1;
		}

		@Override
		public Boolean deserialize(ByteBuffer buffer) {
			var b = buffer.get();
			return b == -1 ? null : b == 1 ? Boolean.TRUE : Boolean.FALSE;
		}

		@Override
		public int compare(ByteBuffer buffer, Boolean element) {
			var b = buffer.get(buffer.position());
			if (b == -1)
				return element == null ? 0 : -1;
			if (element == null)
				return 1;
			return Boolean.compare(b == 1, element);
		}
	};

	ByteConverter<Instant> INSTANT = new ByteConverter<>() {

		@Override
		public byte[] serialize(Instant element) {
			var b = ByteBuffer.allocate(Long.BYTES);
			b.putLong(element != null ? element.toEpochMilli() : -1);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Long.BYTES;
		}

		@Override
		public Instant deserialize(ByteBuffer buffer) {
			var l = buffer.getLong();
			return l != -1 ? Instant.ofEpochMilli(l) : null;
		}

		@Override
		public int compare(ByteBuffer buffer, Instant element) {
			var l = buffer.getLong(buffer.position());
			if (l == -1)
				return element == null ? 0 : -1;
			return element != null ? Long.compare(l, element.toEpochMilli()) : 1;
		}
	};

	ByteConverter<Integer> INTEGER = new ByteConverter<>() {

		@Override
		public byte[] serialize(Integer element) {
			var b = ByteBuffer.allocate(Integer.BYTES);
			b.putInt(element);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Integer.BYTES;
		}

		@Override
		public Integer deserialize(ByteBuffer buffer) {
			return buffer.getInt();
		}

		@Override
		public int compare(ByteBuffer buffer, Integer element) {
			return Integer.compare(buffer.getInt(buffer.position()), element);
		}
	};

	ByteConverter<LocalDate> LOCAL_DATE = new ByteConverter<>() {

		@Override
		public byte[] serialize(LocalDate element) {
			var b = ByteBuffer.allocate(Integer.BYTES + 2 * Byte.BYTES);
			b.putInt(element != null ? element.getYear() : -1);
			b.put((byte) (element != null ? element.getMonthValue() : -1));
			b.put((byte) (element != null ? element.getDayOfMonth() : -1));
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Integer.BYTES + 2 * Byte.BYTES;
		}

		@Override
		public LocalDate deserialize(ByteBuffer buffer) {
			int y = buffer.getInt();
			byte m = buffer.get();
			byte d = buffer.get();
			return y != -1 || m != -1 || d != -1 ? LocalDate.of(y, m, d) : null;
		}

		@Override
		public int compare(ByteBuffer buffer, LocalDate element) {
			var y = buffer.getInt(buffer.position());
			var m = buffer.get(buffer.position() + Integer.BYTES);
			var d = buffer.get(buffer.position() + Integer.BYTES + Byte.BYTES);
			if (y == -1 && m == -1 && d == -1)
				return element == null ? 0 : -1;
			if (element == null)
				return 1;
			var c = Integer.compare(y, element.getYear());
			if (c != 0)
				return c;
			c = Byte.compare(m, (byte) element.getMonthValue());
			return c != 0 ? c : Byte.compare(d, (byte) element.getDayOfMonth());
		}
	};

	ByteConverter<Long> LONG = new ByteConverter<>() {

		@Override
		public byte[] serialize(Long element) {
			var b = ByteBuffer.allocate(Long.BYTES);
			b.putLong(element);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return Long.BYTES;
		}

		@Override
		public Long deserialize(ByteBuffer buffer) {
			return buffer.getLong();
		}

		@Override
		public int compare(ByteBuffer buffer, Long element) {
			return Long.compare(buffer.getLong(buffer.position()), element);
		}
	};

	ByteConverter<Object> NULL = new ByteConverter<>() {

		@Override
		public byte[] serialize(Object element) {
			return new byte[0];
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 0;
		}

		@Override
		public Object deserialize(ByteBuffer buffer) {
			return null;
		}

		@Override
		public int compare(ByteBuffer buffer, Object element) {
			return 0;
		}
	};

	ByteConverter<String> STRING = new ByteConverter<>() {

		@Override
		public byte[] serialize(String element) {
			var bb = element != null ? element.getBytes() : null;
			var b = ByteBuffer.allocate(Integer.BYTES + (bb != null ? bb.length : 0));
			b.putInt(bb != null ? bb.length : -1);
			if (bb != null)
				b.put(bb);
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			var l = buffer.getInt(buffer.position());
			return Integer.BYTES + (l != -1 ? l : 0);
		}

		@Override
		public String deserialize(ByteBuffer buffer) {
			var l = buffer.getInt();
			if (l == -1)
				return null;
			var bb = new byte[l];
			buffer.get(bb);
			return new String(bb);
		}

		@Override
		public int compare(ByteBuffer buffer, String element) {
			var p = buffer.position();
			var l = buffer.getInt(p);
			if (l == -1)
				return element == null ? 0 : -1;
			if (element == null)
				return 1;
			if (l < 0 || p + Integer.BYTES + l > buffer.limit()) {
//				System.out.println("l=" + l);
				throw new RuntimeException();
			}
			var bb = new byte[l];
			buffer.get(p + Integer.BYTES, bb);
			return new String(bb).compareTo(element);
		}
	};

	ByteConverter<URI> URI1 = new ByteConverter<>() {

		@Override
		public byte[] serialize(URI element) {
			return STRING.serialize(element != null ? element.toString() : null);
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return STRING.getLength(buffer);
		}

		@Override
		public URI deserialize(ByteBuffer buffer) {
			var s = STRING.deserialize(buffer);
			return s != null ? URI.create(s) : null;
		}

		@Override
		public int compare(ByteBuffer buffer, URI element) {
			return STRING.compare(buffer, element != null ? element.toString() : null);
		}
	};

	ByteConverter<UUID> UUID1 = new ByteConverter<>() {

		@Override
		public byte[] serialize(UUID element) {
			var b = ByteBuffer.allocate(2 * Long.BYTES);
			b.putLong(element.getMostSignificantBits());
			b.putLong(element.getLeastSignificantBits());
			return b.array();
		}

		@Override
		public int getLength(ByteBuffer buffer) {
			return 2 * Long.BYTES;
		}

		@Override
		public UUID deserialize(ByteBuffer buffer) {
			return new UUID(buffer.getLong(), buffer.getLong());
		}

		@Override
		public int compare(ByteBuffer buffer, UUID element) {
			var c = Long.compare(buffer.getLong(buffer.position()), element.getMostSignificantBits());
			return c != 0 ? c
					: Long.compare(buffer.getLong(buffer.position() + Long.BYTES), element.getLeastSignificantBits());
		}
	};

	public record TypeAndOrder(Class<?> type, SortOrder order) {
	}

	public enum SortOrder {

		ASCENDING, DESCENDING
	}
}
