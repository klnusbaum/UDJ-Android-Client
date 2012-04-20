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
import java.util.ArrayList;

import android.util.Log;
import android.os.Bundle;

public class Player{
  public static final String ID_PARAM ="id";
  public static final String NAME_PARAM="name";
  public static final String HOST_NAME_PARAM="host_username";
  public static final String HOST_ID_PARAM="host_id";
  public static final String LATITUDE_PARAM="latitude";
  public static final String LONGITUDE_PARAM="longitude";
  public static final String HAS_PASSWORD_PARAM="has_password";

  private long playerId;
  private String name;
  private String hostName;
  private long hostId;
  private double latitude;
  private double longitude;
  private boolean hasPassword;


  public Player(
    long playerId, 
    String name, 
    String hostName,
    long hostId, 
    double latitude, 
    double longitude,
    boolean hasPassword)
  {
    this.playerId = playerId;
    this.name = name;
    this.hostName = hostName;
    this.hostId = hostId;
    this.latitude = latitude;
    this.longitude = longitude;
    this.hasPassword = hasPassword;
  }

  public long getPlayerId(){
    return playerId;
  }

  public String getName(){
    return name;
  }

  public String getHostName(){
    return hostName;
  }

  public long getHostId(){
    return hostId;
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
    return new Player(
      jObj.getLong(ID_PARAM),
      jObj.getString(NAME_PARAM),
      jObj.getString(HOST_NAME_PARAM),
      jObj.getLong(HOST_ID_PARAM),
      jObj.optDouble(LATITUDE_PARAM, -100.0),
      jObj.optDouble(LONGITUDE_PARAM, -100.0),
      jObj.getBoolean(HAS_PASSWORD_PARAM));
  }

  public static JSONObject getJSONObject(Player player)
    throws JSONException
  {
    JSONObject toReturn = new JSONObject();
    toReturn.put(ID_PARAM, player.getPlayerId());
    toReturn.put(NAME_PARAM, player.getName());
    toReturn.put(HOST_NAME_PARAM, player.getHostName());
    toReturn.put(HOST_ID_PARAM, player.getHostId());
    toReturn.put(LATITUDE_PARAM, player.getLatitude());
    toReturn.put(LONGITUDE_PARAM, player.getLongitude());
    toReturn.put(HAS_PASSWORD_PARAM, player.getHasPassword());
    return toReturn;
  }

  public static JSONArray getJSONArray(List<Player> players)
    throws JSONException
  {
    JSONArray toReturn = new JSONArray();
    for(Player player: players){
      toReturn.put(getJSONObject(player));
    }
    return toReturn;
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
    toReturn.putLong(ID_PARAM, getPlayerId());
    toReturn.putString(NAME_PARAM, getName());
    toReturn.putString(HOST_NAME_PARAM, getHostName());
    toReturn.putLong(HOST_ID_PARAM, getHostId());
    toReturn.putDouble(LATITUDE_PARAM, getLatitude());
    toReturn.putDouble(LONGITUDE_PARAM, getLongitude());
    toReturn.putBoolean(HAS_PASSWORD_PARAM, getHasPassword());
    return toReturn;
  }

  public static Player unbundle(Bundle toUnbundle){
    return new Player(
      toUnbundle.getLong(ID_PARAM),
      toUnbundle.getString(NAME_PARAM),
      toUnbundle.getString(HOST_NAME_PARAM),
      toUnbundle.getLong(HOST_ID_PARAM),
      toUnbundle.getDouble(LATITUDE_PARAM),
      toUnbundle.getDouble(LONGITUDE_PARAM),
      toUnbundle.getBoolean(HAS_PASSWORD_PARAM));
  }

}
