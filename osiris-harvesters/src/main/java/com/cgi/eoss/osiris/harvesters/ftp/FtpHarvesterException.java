package com.cgi.eoss.osiris.harvesters.ftp;

public class FtpHarvesterException extends Exception {
	
	private static final long serialVersionUID = -7161343746435061915L;

	public FtpHarvesterException(String message) {
		super(message);
	}

	public FtpHarvesterException(Throwable e) {
		super(e);
	}

	public FtpHarvesterException(String message, Throwable cause) {
		super(message, cause);
	}

}
