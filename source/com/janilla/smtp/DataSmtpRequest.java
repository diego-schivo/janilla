package com.janilla.smtp;

import java.io.InputStream;

public interface DataSmtpRequest extends SmtpRequest {
	
	String readHeader();
	
	InputStream getBody();
}
