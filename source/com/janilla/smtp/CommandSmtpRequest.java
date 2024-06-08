package com.janilla.smtp;

public interface CommandSmtpRequest extends SmtpRequest {
	
	String readLine();

	void writeLine(String line);
}
