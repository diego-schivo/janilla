package com.janilla.smtp;

public interface ReplySmtpResponse extends SmtpResponse {

	String readLine();

	void writeLine(String line);
}
