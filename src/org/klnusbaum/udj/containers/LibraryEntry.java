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

import java.util.ArrayList;


public class LibraryEntry{
  public static final String ID_PARAM = "id";
  public static final String TITLE_PARAM = "title";
  public static final String ARTIST_PARAM = "artist";
  public static final String ALBUM_PARAM = "album";
  public static final String DURATION_PARAM = "duration";

  private String libId;
  private String title;
  private String artist;
  private String album;
  private int duration;
  private boolean isAdded;

  public LibraryEntry(
    String libId,
    String title,
    String artist,
    String album,
    int duration)
  {
    this.libId = libId;
    this.title = title;
    this.artist = artist;
    this.album = album;
    this.duration = duration;
    this.isAdded = false;
  }

  public boolean getIsAdded(){
    return isAdded;
  }

  public boolean setIsAdded(boolean isAdded){
    return this.isAdded = isAdded;
  }

  public String getLibId(){
    return libId;
  }

  public String getTitle(){
    return title;
  }

  public String getArtist(){
    return artist;
  }

  public String getAlbum(){
    return album;
  }

  public int getDuration(){
    return duration;
  }

  public boolean equals(Object o){
    LibraryEntry casted = (LibraryEntry)o;
    return casted != null && casted.getLibId().equals(getLibId());
  }

  public static LibraryEntry valueOf(JSONObject jObj)
    throws JSONException 
  {
    return new LibraryEntry(
      jObj.getString(ID_PARAM), 
      jObj.getString(TITLE_PARAM),
      jObj.getString(ARTIST_PARAM),
      jObj.getString(ALBUM_PARAM),
      jObj.getInt(DURATION_PARAM));
  }

  public static ArrayList<LibraryEntry> fromJSONArray(JSONArray array)
    throws JSONException
  {
    ArrayList<LibraryEntry> toReturn = new ArrayList<LibraryEntry>();
    for(int i=0; i<array.length(); i++){
      toReturn.add(valueOf(array.getJSONObject(i)));
    }
    return toReturn;
  }

  public static ArrayList<LibraryEntry> fromRecentlyPlayedJSONArray(JSONArray array)
    throws JSONException
  {
    ArrayList<LibraryEntry> toReturn = new ArrayList<LibraryEntry>();
    for(int i=0; i<array.length(); i++){
      toReturn.add(valueOf(array.getJSONObject(i).getJSONObject("song")));
    }
    return toReturn;

  }

  public String toString(){
    return "Song name: " + getTitle();
  }

}
