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

/**
 * Note we're not keeping track of the time added for the time being.
 * We dont need it at the moment, it's non-trivial to program, and GergorianCalendars
 * take up extra memory.
 */
public class ActivePlaylistEntry{
  public static final String SONG_PARAM = "song";
  public static final String UPVOTERS_PARAM = "upvoters";
  public static final String DOWNVOTERS_PARAM = "downvoters";
  public static final String ADDER_PARAM = "adder";

  public LibraryEntry song;
  public List<User> upvoters;
  public List<User> downvoters;
  public User adder;
  public boolean isCurrentSong;

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
    this.isCurrentSong = false;
  }

  public static ActivePlaylistEntry valueOf(JSONObject jObj){
    return new ActivePlaylistEntry(
      LibraryEntry.valueOf(jObj.getJSONObject(SONG_PARAM)),
      User.fromJSONArray(jObj.getJSONArray(UPVOTERS_PARAM)),
      User.fromJSONArray(jObj.getJSONArray(DOWNVOTERS_PARAM)),
      User.fromJSONArray(jObj.getJSONObject(ADDER_PARAM)));
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

