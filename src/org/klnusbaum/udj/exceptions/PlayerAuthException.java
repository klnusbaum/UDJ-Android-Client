package org.klnusbaum.udj.exceptions;

public class PlayerAuthException extends Exception {
	public static final long serialVersionUID = 1;


	public PlayerAuthException(){
		super();
	}

	public PlayerAuthException(String message){
		super(message);
	}

	public PlayerAuthException(String message, Throwable cause){
		super(message, cause);
	}

	public PlayerAuthException(Throwable cause){
		super(cause);
	}
}
