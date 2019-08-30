package com.cgi.eoss.osiris.harvesters.ftp;

public class FtpPoolException extends RuntimeException {

	private static final long serialVersionUID = -8341631462772456203L;

	public FtpPoolException(String message) {
		super(message);
	}

	public FtpPoolException(Throwable e) {
		super(e);
	}

	public FtpPoolException(String message, Throwable cause) {
		super(message, cause);
	}

}
