package com.janilla.smtp;

public interface SmtpMessage extends AutoCloseable {
	
	void close();
}
