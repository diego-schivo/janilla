package com.janilla.hpack;

public enum Representation {

	INDEXED(7, 0x80), WITH_INDEXING(6, 0x40), WITHOUT_INDEXING(4, 0x00), NEVER_INDEXED(4, 0x10);

	int prefix;

	int first;

	Representation(int prefix, int first) {
		this.prefix = prefix;
		this.first = first;
	}

	public int prefix() {
		return prefix;
	}

	public int first() {
		return first;
	}
}