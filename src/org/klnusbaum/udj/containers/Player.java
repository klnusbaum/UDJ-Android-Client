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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;

import android.os.Bundle;

public class Player implements StringIdable{
  public static final String ID_PARAM ="id";
  public static final String NAME_PARAM="name";
  public static final String LOCATION_PARAM="location";
  public static final String LATITUDE_PARAM="latitude";
  public static final String LONGITUDE_PARAM="longitude";
  public static final String HAS_PASSWORD_PARAM="has_password";
  public static final String OWNER_PARAM = "owner";

  private String playerId;
  private String name;
  private User owner;
  private double latitude;
  private double longitude;
  private boolean hasPassword;


  public Player(
    String playerId, 
    String name, 
    User owner,
    double latitude, 
    double longitude,
    boolean hasPassword)
  {
    this.playerId = playerId;
    this.name = name;
    this.owner = owner;
    this.latitude = latitude;
    this.longitude = longitude;
    this.hasPassword = hasPassword;
  }

  public String getId(){
    return playerId;
  }

  public String getName(){
    return name;
  }

  public User getOwner(){
    return owner;
  }

  public double getLatitude(){
    return latitude;
  }

  public double getLongitude(){
    return longitude;
  }

  public boolean getHasPassword(){
    return hasPassword;
  }

  public static Player valueOf(JSONObject jObj)
    throws JSONException 
  {
    JSONObject ownerObject = jObj.getJSONObject(OWNER_PARAM);
    JSONObject locationObject = jObj.optJSONObject(LOCATION_PARAM);
    return new Player(
      jObj.getString(ID_PARAM),
      jObj.getString(NAME_PARAM),
      User.valueOf(ownerObject),
      locationObject != null ? locationObject.optDouble(LATITUDE_PARAM, -100.0) :  -100.0,
      locationObject != null ? locationObject.optDouble(LONGITUDE_PARAM, -100.0) : -100.0,
      jObj.getBoolean(HAS_PASSWORD_PARAM));
  }

  public static ArrayList<Player> fromJSONArray(JSONArray array)
    throws JSONException
  {
    ArrayList<Player> toReturn = new ArrayList<Player>();
    for(int i=0; i < array.length(); ++i){
      toReturn.add(Player.valueOf(array.getJSONObject(i)));
    }
    return toReturn;
  }

  public Bundle bundleUp(){
    Bundle toReturn = new Bundle();
    toReturn.putString(ID_PARAM, getId());
    toReturn.putString(NAME_PARAM, getName());
    toReturn.putBundle(OWNER_PARAM, getOwner().bundleUp());
    toReturn.putDouble(LATITUDE_PARAM, getLatitude());
    toReturn.putDouble(LONGITUDE_PARAM, getLongitude());
    toReturn.putBoolean(HAS_PASSWORD_PARAM, getHasPassword());
    return toReturn;
  }

  public static Player unbundle(Bundle toUnbundle){
    return new Player(
      toUnbundle.getString(ID_PARAM),
      toUnbundle.getString(NAME_PARAM),
      User.unbundle(toUnbundle.getBundle(OWNER_PARAM)),
      toUnbundle.getDouble(LATITUDE_PARAM),
      toUnbundle.getDouble(LONGITUDE_PARAM),
      toUnbundle.getBoolean(HAS_PASSWORD_PARAM));
  }

}
