package com.janilla.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public interface HttpWritableByteChannel extends WritableByteChannel {

	public int write(ByteBuffer src, boolean endStream) throws IOException;
}
