package org.klnusbaum.udj.exceptions;

public class NoLongerInPlayerException extends Exception {
  public static final long serialVersionUID = 1;

  public NoLongerInPlayerException(){
    super();
  }

  public NoLongerInPlayerException(String message){
    super(message);
  }

  public NoLongerInPlayerException(String message, Throwable cause){
    super(message, cause);
  }

  public NoLongerInPlayerException(Throwable cause){
    super(cause);
  }
}
