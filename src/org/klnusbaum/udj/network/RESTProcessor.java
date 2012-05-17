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
package org.klnusbaum.udj.network;

import java.util.List;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.util.Log;
import android.os.RemoteException;

import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.UDJPlayerProvider;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


public class RESTProcessor{

  public static final String TAG = "RESTProcessor";

  public static void setActivePlaylist(
    JSONObject activePlaylist,
    Context context)
    throws RemoteException, OperationApplicationException, JSONException
  {
    final ContentResolver resolver = context.getContentResolver();
    ArrayList<ContentProviderOperation> batchOps = new ArrayList<ContentProviderOperation>();
    JSONArray playlistEntries = activePlaylist.getJSONArray("active_playlist");
    JSONObject currentSong = activePlaylist.getJSONObject("current_song");

    clearPlaylistAndVotesTable(resolver);

    if(currentSong.length() != 0){
      batchOps.addAll(getPlaylistInsertOps(currentSong, 0, true));
    }

    int priority = 1;
    JSONObject currentEntry;
    for(int i=0; i<playlistEntries.length(); i++){
      currentEntry = playlistEntries.getJSONObject(i);
      batchOps.addAll(getPlaylistInsertOps(currentEntry, priority, false));
      if(batchOps.size() >= 50){
        resolver.applyBatch(Constants.AUTHORITY, batchOps);
        batchOps.clear();
      }
      priority++;
    }
    if(batchOps.size() > 0){
      resolver.applyBatch(Constants.AUTHORITY, batchOps);
      batchOps.clear();
    }
    resolver.notifyChange(UDJPlayerProvider.PLAYLIST_URI, null);
    resolver.notifyChange(UDJPlayerProvider.VOTES_URI, null);
  }

  private static void clearPlaylistAndVotesTable(ContentResolver cr) {
    cr.delete(UDJPlayerProvider.PLAYLIST_URI, null, null);
    cr.delete(UDJPlayerProvider.VOTES_URI, null, null);
  }

  private static List<ContentProviderOperation> getPlaylistInsertOps(
    JSONObject entry, int priority, boolean isCurrentSong)
    throws JSONException
  {
    JSONObject song = entry.getJSONObject("song");
    JSONObject adder = entry.getJSONObject("adder");
    final ContentProviderOperation.Builder songInsertOp = 
      ContentProviderOperation.newInsert(UDJPlayerProvider.PLAYLIST_URI)
      .withValue(UDJPlayerProvider.LIB_ID_COLUMN, song.getInt("id"))
      .withValue(UDJPlayerProvider.TIME_ADDED_COLUMN, 
          entry.getString("time_added"))
      .withValue(UDJPlayerProvider.PRIORITY_COLUMN, priority)
      .withValue(UDJPlayerProvider.TITLE_COLUMN, song.getString("title"))
      .withValue(UDJPlayerProvider.ARTIST_COLUMN, song.getString("artist"))
      .withValue(UDJPlayerProvider.ALBUM_COLUMN, song.getString("album"))
      .withValue(UDJPlayerProvider.DURATION_COLUMN, song.getInt("duration"))
      .withValue(UDJPlayerProvider.ADDER_ID_COLUMN, adder.getLong("id"))
      .withValue(UDJPlayerProvider.ADDER_USERNAME_COLUMN, adder.getString("username"))
      .withValue(UDJPlayerProvider.IS_CURRENTLY_PLAYING_COLUMN, isCurrentSong ? 1 : 0);
    ArrayList<ContentProviderOperation> toReturn = new ArrayList<ContentProviderOperation>();
    toReturn.add(songInsertOp.build());
    JSONArray upVoters = entry.getJSONArray("upvoters");
    for(int i=0; i<upVoters.length(); ++i){
      final ContentProviderOperation.Builder voteInsertOp = 
        ContentProviderOperation.newInsert(UDJPlayerProvider.VOTES_URI)
        .withValue(UDJPlayerProvider.LIB_ID_COLUMN, song.getInt("id"))
        .withValue(UDJPlayerProvider.VOTE_WEIGHT_COLUMN, 1)
        .withValue(UDJPlayerProvider.VOTER_ID_COLUMN, upVoters.getJSONObject(i).getInt("id"));
      toReturn.add(voteInsertOp.build());
    }
    JSONArray downVoters = entry.getJSONArray("downvoters");
    for(int i=0; i<downVoters.length(); ++i){
      final ContentProviderOperation.Builder voteInsertOp = 
        ContentProviderOperation.newInsert(UDJPlayerProvider.VOTES_URI)
        .withValue(UDJPlayerProvider.LIB_ID_COLUMN, song.getInt("id"))
        .withValue(UDJPlayerProvider.VOTE_WEIGHT_COLUMN, -1)
        .withValue(UDJPlayerProvider.VOTER_ID_COLUMN, downVoters.getJSONObject(i).getInt("id"));
      toReturn.add(voteInsertOp.build());
    }
    return toReturn;
  }

}
