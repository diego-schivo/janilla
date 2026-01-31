package com.janilla.net;

import java.io.IOException;

public interface Transfer {

	int read() throws IOException;

	void write() throws IOException;
}
