/**
 * Copyright 2011 Kurtis L. Nusbaum
 *
 * This file is part of UDJ.
 *
 * UDJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * UDJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with UDJ.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.klnusbaum.udj.exceptions;

/**
 * Thrown when there's an Http 409 response
 */
public class ConflictException extends Exception{

  public static final long serialVersionUID = 1;

  public ConflictException(){
    super();
  }

  public ConflictException(String message){
    super(message);
  }

  public ConflictException(String message, Throwable cause){
    super(message, cause);
  }

  public ConflictException(Throwable cause){
    super(cause);
  }
}
