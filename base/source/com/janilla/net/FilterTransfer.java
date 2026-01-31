package com.janilla.net;

import java.io.IOException;

public class FilterTransfer implements Transfer {

	protected final Transfer transfer;

	public FilterTransfer(Transfer transfer) {
		this.transfer = transfer;
	}

	public Transfer transfer() {
		return transfer;
	}

	@Override
	public int read() throws IOException {
		return transfer.read();
	}

	@Override
	public void write() throws IOException {
		transfer.write();
	}
}
