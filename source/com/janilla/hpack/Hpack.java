package com.janilla.hpack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class Hpack {

	static StaticTable staticTable = new StaticTable();

	public static void main(String[] args) {
		var e = new Encoder();

		System.out.println(toBinaryString(e.encodeInteger(10, 0, 5)));
		System.out.println(toBinaryString(e.encodeInteger(1337, 0, 5)));
		System.out.println(toBinaryString(e.encodeInteger(42, 0, 8)));

		var h = new Header("custom-key", "custom-header");
		System.out.println(h);
		var s = toHexString(e.encodeHeader(h));
		System.out.println(s);
		var d = new Decoder();
		d.decodeHeader(parseHex(s).iterator());
		System.out.println(d.headers);
		System.out.println(d.dynamicTable);

		h = new Header(":path", "/sample/path");
		System.out.println(h);
		s = toHexString(e.encodeHeader(h));
		System.out.println(s);
		d = new Decoder();
		d.decodeHeader(parseHex(s).iterator());
		System.out.println(d.headers);
		System.out.println(d.dynamicTable);

		h = new Header("password", "secret");
		System.out.println(h);
		s = toHexString(e.encodeHeader(h));
		System.out.println(s);
		d = new Decoder();
		d.decodeHeader(parseHex(s).iterator());
		System.out.println(d.headers);
		System.out.println(d.dynamicTable);

		h = new Header(":method", "GET");
		System.out.println(h);
		s = toHexString(e.encodeHeader(h));
		System.out.println(s);
		d = new Decoder();
		d.decodeHeader(parseHex(s).iterator());
		System.out.println(d.headers);
		System.out.println(d.dynamicTable);
	}

	static String toBinaryString(IntStream integers) {
		return integers.mapToObj(Integer::toBinaryString).collect(Collectors.joining(" "));
	}

	static String toHexString(IntStream integers) {
		return integers.mapToObj(x -> {
			var s = Integer.toHexString(x);
			return s.length() == 1 ? "0" + s : s;
		}).collect(Collector.of(StringBuilder::new, (b, x) -> {
			if (b.length() % 5 == 4)
				b.append(b.length() % 40 == 39 ? '\n' : ' ');
			b.append(x);
		}, (b1, b2) -> b1, StringBuilder::toString));
	}

	static IntStream parseHex(String string) {
		var e = (string.length() - (string.length() / 5)) / 2;
		return IntStream.range(0, e).map(x -> {
			var i = x * 2;
			i += i / 4;
			return Integer.parseInt(string.substring(i, i + 2), 16);
		});
	}

	static class Decoder {

		List<Header> headers = new ArrayList<>();

		List<Header> dynamicTable = new ArrayList<>();

		void decodeHeader(PrimitiveIterator.OfInt bytes) {
			var b = bytes.nextInt();
			if ((b & 0x80) != 0) {
				var i = decodeInteger(
						IntStream
								.concat(IntStream.of(b),
										StreamSupport.intStream(
												Spliterators.spliteratorUnknownSize(bytes, Spliterator.ORDERED), false))
								.iterator(),
						7);
				var h = staticTable.header(i);
				headers.add(h);
			} else if ((b & 0x40) != 0) {
				if ((b & 0x3f) != 0) {
					var i = decodeInteger(IntStream
							.concat(IntStream.of(b),
									StreamSupport.intStream(
											Spliterators.spliteratorUnknownSize(bytes, Spliterator.ORDERED), false))
							.iterator(), 6);
					var h0 = staticTable.header(i);
					String v;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						v = new String(cc);
					}
					var h = new Header(h0.name(), v);
					headers.add(h);
					dynamicTable.add(h);
				} else {
					String n;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						n = new String(cc);
					}
					String v;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						v = new String(cc);
					}
					var h = new Header(n, v);
					headers.add(h);
					dynamicTable.add(h);
				}
			} else if ((b & 0xf0) == 0) {
				if ((b & 0x0f) != 0) {
					var i = decodeInteger(IntStream
							.concat(IntStream.of(b),
									StreamSupport.intStream(
											Spliterators.spliteratorUnknownSize(bytes, Spliterator.ORDERED), false))
							.iterator(), 4);
					var h0 = staticTable.header(i);
					String v;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						v = new String(cc);
					}
					var h = new Header(h0.name(), v);
					headers.add(h);
				} else {
					String n;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						n = new String(cc);
					}
					String v;
					{
						var l = bytes.nextInt();
						var cc = new char[l];
						IntStream.range(0, l).forEach(x -> cc[x] = (char) bytes.nextInt());
						v = new String(cc);
					}
					var h = new Header(n, v);
					headers.add(h);
				}
			}
		}

		int decodeInteger(PrimitiveIterator.OfInt bytes, int prefix) {
			var z = 1 << prefix;
			var b = bytes.nextInt();
			var i = b & (z - 1);
			if (i < z - 1)
				return i;
			var m = 0;
			do {
				b = bytes.nextInt();
				i += (b & 0x7f) << m;
				m += 7;
			} while ((b & 0x80) != 0);
			return i;
		}
	}

	static class Encoder {

		IntStream encodeHeader(Header header) {
			var b = IntStream.builder();
			var hh = staticTable.headers(header.name());
			IndexAndHeader h;
			if (hh != null) {
				h = hh.stream().filter(x -> x.header().equals(header)).findFirst().orElse(null);
				if (h != null)
					encodeInteger(h.index(), 0x80, 7).forEach(b::add);
				else
					b.add(hh.get(0).index());
			} else {
				h = null;
				b.add(0x40);
				b.add(header.name().length());
				header.name().chars().forEach(b::add);
			}
			if (h == null) {
				b.add(header.value().length());
				header.value().chars().forEach(b::add);
			}
			return b.build();
		}

		IntStream encodeInteger(int value, int first, int prefix) {
			var z = 1 << prefix;
			first &= -1 ^ (z - 1);
			if (value < z - 1)
				return IntStream.of(first | (value & (z - 1)));
			var b = IntStream.builder();
			b.add(first | (z - 1));
			value -= (z - 1);
			var y = 1 << 7;
			while (value >= y) {
				b.add(y | (value & (y - 1)));
				value >>>= 7;
			}
			b.add(value);
			return b.build();
		}
	}

	record Header(String name, String value) {
	}

	record IndexAndHeader(int index, Header header) {
	};

	static class StaticTable {

		List<Header> list;

		Map<String, List<IndexAndHeader>> map;

		public StaticTable() {
			list = """
					:authority=
					:method=GET
					:method=POST
					:path=/
					:path=/index.html
					:scheme=http
					:scheme=https
					:status=200
					:status=204
					:status=206
					:status=304
					:status=400
					:status=404
					:status=500
					accept-charset=
					accept-encoding=gzip=
					accept-language=
					accept-ranges=
					accept=
					access-control-allow-origin=
					age=
					allow=
					authorization=
					cache-control=
					content-disposition=
					content-encoding=
					content-language=
					content-length=
					content-location=
					content-range=
					content-type=
					cookie=
					date=
					etag=
					expect=
					expires=
					from=
					host=
					if-match=
					if-modified-since=
					if-none-match=
					if-range=
					if-unmodified-since=
					last-modified=
					link=
					location=
					max-forwards=
					proxy-authenticate=
					proxy-authorization=
					range=
					referer=
					refresh=
					retry-after=
					server=
					set-cookie=
					strict-transport-security=
					transfer-encoding=
					user-agent=
					vary=
					via=
					www-authenticate=""".lines().map(x -> {
				var ss = x.split("=", 2);
				return new Header(ss[0], ss[1]);
			}).toList();

			map = list.stream().collect(
					Collector.of(HashMap::new, new BiConsumer<HashMap<String, List<IndexAndHeader>>, Header>() {

						int index;

						@Override
						public void accept(HashMap<String, List<IndexAndHeader>> t, Header u) {
							t.compute(u.name(), (k, v) -> {
								if (v == null)
									v = new ArrayList<>();
								v.add(new IndexAndHeader(++index, u));
								return v;
							});
						}
					}, (m1, m2) -> m1));

		}

		Header header(int index) {
			var i = index - 1;
			return i >= 0 && i < list.size() ? list.get(index - 1) : null;
		}

		List<IndexAndHeader> headers(String name) {
			return map.get(name);
		}
	}
}
