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
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import org.klnusbaum.udj.R;
import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.UDJPlayerProvider;
import org.klnusbaum.udj.containers.LibraryEntry;
import org.klnusbaum.udj.network.ServerConnection;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


public class RESTProcessor{

  public static final String TAG = "RESTProcessor";

  private static void setCurrentSong(JSONObject currentSong, ContentResolver cr)
    throws JSONException
  {
    cr.delete(UDJPlayerProvider.CURRENT_SONG_URI, null, null); 
    ContentValues toInsert = new ContentValues();
    toInsert.put(
      UDJPlayerProvider.PLAYLIST_ID_COLUMN, currentSong.getLong("id"));
    toInsert.put(
      UDJPlayerProvider.UP_VOTES_COLUMN, currentSong.getInt("up_votes"));
    toInsert.put(
      UDJPlayerProvider.DOWN_VOTES_COLUMN, currentSong.getInt("down_votes"));
    toInsert.put(
      UDJPlayerProvider.TIME_ADDED_COLUMN, currentSong.getString("time_added"));
    toInsert.put(
      UDJPlayerProvider.TIME_PLAYED_COLUMN, 
      currentSong.getString("time_played"));
    toInsert.put(
      UDJPlayerProvider.DURATION_COLUMN, currentSong.getInt("duration"));
    toInsert.put(
      UDJPlayerProvider.TITLE_COLUMN, currentSong.getString("title"));
    toInsert.put(
      UDJPlayerProvider.ARTIST_COLUMN, currentSong.getString("artist"));
    toInsert.put(
      UDJPlayerProvider.ALBUM_COLUMN, currentSong.getString("album"));
    toInsert.put(
      UDJPlayerProvider.ADDER_ID_COLUMN, currentSong.getLong("adder_id"));
    toInsert.put(
      UDJPlayerProvider.ADDER_USERNAME_COLUMN, 
      currentSong.getString("adder_username"));
    cr.insert(UDJPlayerProvider.CURRENT_SONG_URI, toInsert);
    cr.notifyChange(UDJPlayerProvider.CURRENT_SONG_URI, null);
  }

  public static void setActivePlaylist(
    JSONObject activePlaylist,
    Context context)
    throws RemoteException, OperationApplicationException, JSONException
  {
    JSONArray playlistEntries = activePlaylist.getJSONArray("active_playlist");
    JSONObject currentSong = activePlaylist.getJSONObject("current_song");
    final ContentResolver resolver = context.getContentResolver();

    if(currentSong.length() > 0){
      setCurrentSong(currentSong, resolver);
    }

    deleteRemovedPlaylistEntries(playlistEntries, resolver);
    
    ArrayList<ContentProviderOperation> batchOps = 
      new ArrayList<ContentProviderOperation>();
    Set<Long> needUpdate = getNeedUpdatePlaylistEntries(resolver);

    int priority = 1;
    JSONObject currentEntry;
    for(int i=0; i<playlistEntries.length(); i++){
      currentEntry = playlistEntries.getJSONObject(i);
      long id = currentEntry.getLong("id");
      if(needUpdate.contains(id)){
        batchOps.add(getPlaylistPriorityUpdate(currentEntry, priority));
      }
      else{
        batchOps.add(getPlaylistInsertOp(currentEntry, priority)); 
      }
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
  }

  private static Set<Long> getNeedUpdatePlaylistEntries(ContentResolver cr){
    HashSet<Long> toReturn = new HashSet<Long>();
    Cursor currentPlaylist = cr.query(
      UDJPlayerProvider.PLAYLIST_URI, 
      new String[]{UDJPlayerProvider.PLAYLIST_ID_COLUMN},
      null, null, null);
    if(currentPlaylist.moveToFirst()){
      int playlistIdColumn = 
        currentPlaylist.getColumnIndex(UDJPlayerProvider.PLAYLIST_ID_COLUMN);
      do{
        toReturn.add(currentPlaylist.getLong(playlistIdColumn));
      }while(currentPlaylist.moveToNext());
    }
    currentPlaylist.close();
    return toReturn;
  }


  private static void deleteRemovedPlaylistEntries(
    JSONArray playlistEntries, ContentResolver cr)
    throws JSONException
  {
    if(playlistEntries.length() ==0){
      return;
    }

    String where = "";
    String[] selectionArgs = new String[playlistEntries.length()];
    int i;
    for(i=0; i<playlistEntries.length()-1; i++){
      where += UDJPlayerProvider.PLAYLIST_ID_COLUMN + "!=? AND ";
      selectionArgs[i] = playlistEntries.getJSONObject(i).getString("id");
    }
    selectionArgs[i] = playlistEntries.getJSONObject(i).getString("id");
    where += UDJPlayerProvider.PLAYLIST_ID_COLUMN + "!=?";  

    cr.delete(UDJPlayerProvider.PLAYLIST_URI, where, selectionArgs); 
  }

  private static ContentProviderOperation getPlaylistInsertOp(
    JSONObject entry, int priority)
    throws JSONException
  {
    final ContentProviderOperation.Builder insertOp = 
      ContentProviderOperation.newInsert(UDJPlayerProvider.PLAYLIST_URI)
      .withValue(UDJPlayerProvider.PLAYLIST_ID_COLUMN, entry.getLong("id"))
      .withValue(UDJPlayerProvider.UP_VOTES_COLUMN, entry.getInt("up_votes"))
      .withValue(UDJPlayerProvider.DOWN_VOTES_COLUMN, entry.getInt("down_votes"))
      .withValue(UDJPlayerProvider.TIME_ADDED_COLUMN, 
        entry.getString("time_added"))
      .withValue(UDJPlayerProvider.PRIORITY_COLUMN, priority)
      .withValue(UDJPlayerProvider.TITLE_COLUMN, entry.getString("title"))
      .withValue(UDJPlayerProvider.ARTIST_COLUMN, entry.getString("artist"))
      .withValue(UDJPlayerProvider.ALBUM_COLUMN, entry.getString("album"))
      .withValue(UDJPlayerProvider.DURATION_COLUMN, entry.getInt("duration"))
      .withValue(UDJPlayerProvider.ADDER_ID_COLUMN, entry.getLong("adder_id"))
      .withValue(UDJPlayerProvider.ADDER_USERNAME_COLUMN, 
        entry.getString("adder_username"));
    return insertOp.build();
  }

  private static ContentProviderOperation getPlaylistPriorityUpdate(
    JSONObject currentEntry, int priority)
    throws JSONException
  {
    final ContentProviderOperation.Builder updateOp = 
      ContentProviderOperation.newUpdate(UDJPlayerProvider.PLAYLIST_URI)
      .withSelection(
        UDJPlayerProvider.PLAYLIST_ID_COLUMN + 
        "=" + currentEntry.getInt("id"), null)
      .withValue(UDJPlayerProvider.PRIORITY_COLUMN, String.valueOf(priority))
      .withValue(
        UDJPlayerProvider.UP_VOTES_COLUMN, currentEntry.getInt("up_votes"))
      .withValue(
        UDJPlayerProvider.DOWN_VOTES_COLUMN, currentEntry.getInt("down_votes"));
    return updateOp.build();
  }

  public static void setPlaylistAddRequestsSynced(
    Set<Long> requestIds,
    Context context)
    throws RemoteException, OperationApplicationException
  {
    final ContentResolver resolver = context.getContentResolver();
    ArrayList<ContentProviderOperation> batchOps = 
      new ArrayList<ContentProviderOperation>();
    for(Long requestId : requestIds){
      batchOps.add(getAddRequestSyncedOp(requestId));
      if(batchOps.size() >= 50){
        resolver.applyBatch(Constants.AUTHORITY, batchOps);
        batchOps.clear();
      }
    }
    if(batchOps.size() > 0){
      resolver.applyBatch(Constants.AUTHORITY, batchOps);
      batchOps.clear();
    }
  }

  private static ContentProviderOperation getAddRequestSyncedOp(long requestId){
    final ContentProviderOperation.Builder updateBuilder = 
      ContentProviderOperation.newUpdate(
        UDJPlayerProvider.PLAYLIST_ADD_REQUEST_URI)
      .withSelection(
        UDJPlayerProvider.ADD_REQUEST_ID_COLUMN + "=" +String.valueOf(requestId),
        null)
      .withValue(UDJPlayerProvider.ADD_REQUEST_SYNC_STATUS_COLUMN,
         UDJPlayerProvider.ADD_REQUEST_SYNCED);
    return updateBuilder.build();
  }

  public static void setPlaylistRemoveRequestsSynced(
      List<Long> plIds, Context context)
    throws RemoteException, OperationApplicationException
  {
    Log.d(TAG, "Sycning playlist remove statuses");
    final ContentResolver resolver = context.getContentResolver();
    ArrayList<ContentProviderOperation> batchOps = 
      new ArrayList<ContentProviderOperation>();
    for(Long plId : plIds){
      batchOps.add(getRemoveRequestSyncedOp(plId));
      if(batchOps.size() >= 50){
        resolver.applyBatch(Constants.AUTHORITY, batchOps);
        batchOps.clear();
      }
    }
    if(batchOps.size() > 0){
      resolver.applyBatch(Constants.AUTHORITY, batchOps);
      batchOps.clear();
    }
  }

  private static ContentProviderOperation getRemoveRequestSyncedOp(long plId){
    final ContentProviderOperation.Builder updateBuilder = 
      ContentProviderOperation.newUpdate(
        UDJPlayerProvider.PLAYLIST_REMOVE_REQUEST_URI)
      .withSelection(
        UDJPlayerProvider.REMOVE_REQUEST_PLAYLIST_ID_COLUMN + "=" +String.valueOf(plId),
        null)
      .withValue(UDJPlayerProvider.ADD_REQUEST_SYNC_STATUS_COLUMN,
         UDJPlayerProvider.REMOVE_REQUEST_SYNCED);
    return updateBuilder.build();
  }


  public static void setVoteRequestsSynced(Cursor voteRequests, Context context)
  {
    ContentResolver cr = context.getContentResolver();
    if(voteRequests.moveToFirst()){
      int idColIndex = 
        voteRequests.getColumnIndex(UDJPlayerProvider.VOTE_ID_COLUMN);
      do{
        //TODO these should be batch operations
        long requestId = voteRequests.getLong(idColIndex);
        ContentValues updatedValue = new ContentValues();
        updatedValue.put(
          UDJPlayerProvider.VOTE_SYNC_STATUS_COLUMN, 
          UDJPlayerProvider.VOTE_SYNCED);
        cr.update(
          UDJPlayerProvider.VOTES_URI,
          updatedValue,
          UDJPlayerProvider.VOTE_ID_COLUMN + "=" + requestId,
          null);
      }while(voteRequests.moveToNext());
    }
  }

}
