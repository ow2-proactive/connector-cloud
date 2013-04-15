package org.ow2.proactive.iaas.monitoring.vmprocesses;

public class VMProcessException extends Exception {

	public VMProcessException() {
	}

	public VMProcessException(String message) {
		super(message);
	}

	public VMProcessException(Throwable cause) {
		super(cause);
	}

	public VMProcessException(String message, Throwable cause) {
		super(message, cause);
	}

}
