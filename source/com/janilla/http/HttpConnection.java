package com.janilla.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janilla.net.Connection;

public class HttpConnection extends Connection {

	private HeaderDecoder headerDecoder = new HeaderDecoder();

	private boolean prefaceReceived;

	private boolean prefaceSent;

	private Map<Integer, List<Frame>> streams = new HashMap<>();

	// *******************
	// Getters and Setters

	public HeaderDecoder getHeaderDecoder() {
		return headerDecoder;
	}

	public void setHeaderDecoder(HeaderDecoder headerDecoder) {
		this.headerDecoder = headerDecoder;
	}

	public boolean isPrefaceReceived() {
		return prefaceReceived;
	}

	public void setPrefaceReceived(boolean prefaceReceived) {
		this.prefaceReceived = prefaceReceived;
	}

	public boolean isPrefaceSent() {
		return prefaceSent;
	}

	public void setPrefaceSent(boolean prefaceSent) {
		this.prefaceSent = prefaceSent;
	}

	public Map<Integer, List<Frame>> getStreams() {
		return streams;
	}

	public void setStreams(Map<Integer, List<Frame>> streams) {
		this.streams = streams;
	}

	// Getters and Setters
	// *******************
}