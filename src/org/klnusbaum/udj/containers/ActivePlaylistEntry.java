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

/**
 * Note we're not keeping track of the time added for the time being.
 * We dont need it at the moment, it's non-trivial to program, and GergorianCalendars
 * take up extra memory.
 */
public class ActivePlaylistEntry implements StringIdable{
  public static final String SONG_PARAM = "song";
  public static final String UPVOTERS_PARAM = "upvoters";
  public static final String DOWNVOTERS_PARAM = "downvoters";
  public static final String ADDER_PARAM = "adder";

  private LibraryEntry song;
  private List<User> upvoters;
  private List<User> downvoters;
  private User adder;
  private boolean currentSong;

  public ActivePlaylistEntry(
    LibraryEntry song,
    List<User> upvoters,
    List<User> downvoters,
    User adder
  ){
    this.song = song;
    this.upvoters = upvoters;
    this.downvoters = downvoters;
    this.adder = adder;
    this.currentSong = false;
  }

  public String getId(){
    return song.getId();
  }

  public LibraryEntry getSong(){
    return song;
  }

  public List<User> getUpvoters(){
    return upvoters;
  }

  public List<User> getDownvoters(){
    return downvoters;
  }

  public void removeFromDownvoters(User toRemove){
    downvoters.remove(toRemove);
  }

  public void removeFromUpvoters(User toRemove){
    upvoters.remove(toRemove);
  }

  public void addToDownvoters(User toAdd){
    downvoters.add(toAdd);
  }

  public void addToUpvoters(User toAdd){
    upvoters.add(toAdd);
  }


  public User getAdder(){
    return adder;
  }

  public boolean isCurrentSong(){
    return currentSong;
  }

  public void setCurrentSong(boolean currentSong){
    this.currentSong = currentSong;
  }

  public boolean equals(Object o){
    ActivePlaylistEntry casted = (ActivePlaylistEntry)o;
    return casted != null && this.getSong().getId().equals(casted.getSong().getId());
  }

  public static ActivePlaylistEntry valueOf(JSONObject jObj)
    throws JSONException
  {
    return new ActivePlaylistEntry(
      LibraryEntry.valueOf(jObj.getJSONObject(SONG_PARAM)),
      User.fromJSONArray(jObj.getJSONArray(UPVOTERS_PARAM)),
      User.fromJSONArray(jObj.getJSONArray(DOWNVOTERS_PARAM)),
      User.valueOf(jObj.getJSONObject(ADDER_PARAM)));
  }

  public static List<ActivePlaylistEntry> fromJSONArray(JSONArray array)
    throws JSONException
  {
    ArrayList<ActivePlaylistEntry> toReturn = new ArrayList<ActivePlaylistEntry>();
    for(int i=0; i<array.length(); i++){
      toReturn.add(valueOf(array.getJSONObject(i)));
    }
    return toReturn;
  }
}

