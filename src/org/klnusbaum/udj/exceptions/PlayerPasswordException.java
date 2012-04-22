package org.klnusbaum.udj.exceptions;

public class PlayerPasswordException extends Exception {
	public PlayerPasswordException(){
		super();
	}

	public PlayerPasswordException(String message){
		super(message);
	}

	public PlayerPasswordException(String message, Throwable cause){
		super(message, cause);
	}

	public PlayerPasswordException(Throwable cause){
		super(cause);
	}
}
