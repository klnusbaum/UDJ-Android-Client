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
package org.klnusbaum.udj.containers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;

import android.os.Bundle;

public class User implements StringIdable{
  public static final String ID_PARAM = "id";
  public static final String USERNAME_PARAM = "username";
  public static final String FIRST_NAME_PARAM = "first_name";
  public static final String LAST_NAME_PARAM = "last_name";

  private String ID;
  private String username;
  private String firstName;
  private String lastName;

  public User(String ID){
    this.ID = ID;
    this.username = "";
    this.firstName = "";
    this.lastName = "";
  }

  public User(String ID, String username, String firstName, String lastName){
    this.ID = ID;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getId(){
    return this.ID;
  }

  public String getUsername(){
    return username;
  }

  public String getFirstName(){
    return firstName;
  }

  public String getLastName(){
    return lastName;
  }

  public boolean equals(Object o){
    return ID.equals(((User)o).ID);
  }

  public static User valueOf(JSONObject jObj)
    throws JSONException
  {
    return new User(
        jObj.getString(ID_PARAM),
        jObj.getString(USERNAME_PARAM),
        jObj.getString(FIRST_NAME_PARAM),
        jObj.getString(LAST_NAME_PARAM));
  }

  public static List<User> fromJSONArray(JSONArray array)
    throws JSONException
  {
    ArrayList<User> toReturn = new ArrayList<User>();
    for(int i=0; i<array.length(); i++){
      toReturn.add(valueOf(array.getJSONObject(i)));
    }
    return toReturn;
  }

  public Bundle bundleUp(){
    Bundle toReturn = new Bundle();
    toReturn.putString(ID_PARAM, getId());
    toReturn.putString(USERNAME_PARAM, getUsername());
    toReturn.putString(FIRST_NAME_PARAM, getFirstName());
    toReturn.putString(LAST_NAME_PARAM, getLastName());
    return toReturn;
  }

  public static User unbundle(Bundle toUnbundle){
    return new User(
      toUnbundle.getString(ID_PARAM),
      toUnbundle.getString(USERNAME_PARAM),
      toUnbundle.getString(FIRST_NAME_PARAM),
      toUnbundle.getString(LAST_NAME_PARAM)
    );
  }


}

