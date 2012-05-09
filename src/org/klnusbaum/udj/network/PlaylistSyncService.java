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


import android.content.Context;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.accounts.AuthenticatorException;
import android.os.RemoteException;
import android.content.OperationApplicationException;
import android.util.Log;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.drawable.BitmapDrawable;


import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.ParseException;

import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.UDJPlayerProvider;
import org.klnusbaum.udj.R;
import org.klnusbaum.udj.exceptions.PlayerAuthException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.Utils;


/**
 * Adapter used to sync up with the UDJ server.
 */
public class PlaylistSyncService extends IntentService{

  private static final int SONG_ADD_EXCEPTION_ID = 1;
  private static final int SONG_REMOVE_EXCEPTION_ID = 2;
  private static final int SONG_SET_EXCEPTION_ID = 3;

  private static final String TAG = "PlaylistSyncService";

  public PlaylistSyncService(){
    super("PlaylistSyncService");
  }

  @Override
  public void onHandleIntent(Intent intent){
    final Account account =
      (Account)intent.getParcelableExtra(Constants.ACCOUNT_EXTRA);
    long playerId = Long.valueOf(AccountManager.get(this).getUserData(
      account, Constants.LAST_PLAYER_ID_DATA));
    //TODO handle error if eventId is bad
    if(intent.getAction().equals(Intent.ACTION_INSERT)){
      if(intent.getData().equals(UDJPlayerProvider.PLAYLIST_URI)){
        long libId = intent.getLongExtra(
          Constants.LIB_ID_EXTRA, 
          UDJPlayerProvider.INVALID_LIB_ID);
        addSongToPlaylist(account, playerId, libId, true, intent);
      }
      else if(intent.getData().equals(UDJPlayerProvider.VOTES_URI)){
        //TODO handle if lib id is bad
        long libId = intent.getLongExtra(Constants.LIB_ID_EXTRA, -1);
        //TODO handle if votetype is bad
        int voteWeight = intent.getIntExtra(Constants.VOTE_WEIGHT_EXTRA,0); 
        voteOnSong(account, playerId, libId, voteWeight, true);
      }
      updateActivePlaylist(account, playerId, true); 
    }
    else if(intent.getAction().equals(Intent.ACTION_VIEW)){
      updateActivePlaylist(account, playerId, true); 
    }
    else if(intent.getAction().equals(Intent.ACTION_DELETE)){
      Log.d(TAG, "Handling delete");
      if(intent.getData().equals(UDJPlayerProvider.PLAYLIST_URI)){
        Log.d(TAG, "In plalist syncservice, about to insert song into remove requests");
        //TODO handle if Playlist id is bad.
        long libId = intent.getLongExtra(Constants.LIB_ID_EXTRA, -1);
        removeSongFromPlaylist(account, playerId, libId, true, intent);
      }
      updateActivePlaylist(account, playerId, true);
    }
    else if(intent.getAction().equals(Constants.ACTION_SET_CURRENT_SONG)){
      Log.d(TAG, "Handling setting current song");
      long libId = intent.getLongExtra(Constants.LIB_ID_EXTRA, -1);
      setCurrentSong(account, playerId, libId, true, intent);
      updateActivePlaylist(account, playerId, true);
    }
  }

  private void updateActivePlaylist(
    Account account, long playerId, boolean attemptReauth)
  {
    Log.d(TAG, "updating active playlist");
    AccountManager am = AccountManager.get(this);
    String authToken = "";
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when retreiving playist");
    }
    catch(OperationCanceledException e){
      Log.e(TAG, "Op Canceled exception when retreiving playist");
    }
    catch(AuthenticatorException e){
      Log.e(TAG, "Authentication exception when retreiving playist");
    }


    try{
      JSONObject activePlaylist =
        ServerConnection.getActivePlaylist(playerId, authToken);
      RESTProcessor.setActivePlaylist(activePlaylist, this);
    }
    catch(JSONException e){
      Log.e(TAG, "JSON exception when retreiving playist");
      Log.e(TAG, e.getMessage());
    }
    catch(ParseException e){
      Log.e(TAG, "Parse exception when retreiving playist");
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when retreiving playist");
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        Log.e(TAG, "Soft Authentication exception when retreiving playist");
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken); 
        updateActivePlaylist(account, playerId, false);
      }
      else{
        Log.e(TAG, "Hard Authentication exception when retreiving playist");
      } 
    }
    catch(RemoteException e){
      Log.e(TAG, "Remote exception when retreiving playist");
    }
    catch(OperationApplicationException e){
      Log.e(TAG, "Operation Application exception when retreiving playist");
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Player Inactive exception when retreiving playlist");
      Utils.handleInactivePlayer(this, account);
    }
    catch (PlayerAuthException e) {
    //TODO REAUTH AND THEN TRY GETTING PLAYLIST AGAIN
    e.printStackTrace();
  }
    //TODO This point of the app seems very dangerous as there are so many
    // exceptions that could occuer. Need to pay special attention to this.

  }

  private void setCurrentSong(
    Account account,
    long playerId,
    long libId,
    boolean attemptReauth,
    Intent originalIntent)
  {
    String authToken = "";
    AccountManager am = AccountManager.get(this);
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(AuthenticatorException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "Authentication exception when setting song");
    }
    catch(OperationCanceledException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "Op Canceled exception when setting song");
    }
    catch(IOException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "IO exception when geting authtoken for setting song");
      Log.e(TAG, e.getMessage());
    }

    try{
      ServerConnection.setCurrentSong(playerId, libId, authToken);
    }
    catch(IOException e){
      alertSetSongException(account, originalIntent);
      Log.e(TAG, "IO exception when setting song");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken); 
        addSongToPlaylist(account, playerId, libId, false, originalIntent);
        Log.e(TAG, "Soft Authentication exception when setting song");
      }
      else{
        alertSetSongException(account, originalIntent);
        Log.e(TAG, "Hard Authentication exception when setting song");
      }
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when setting song");
      Utils.handleInactivePlayer(this, account);
    } catch (PlayerAuthException e) {
      //TODO REAUTH AND THEN TRY ADD AGAIN
      e.printStackTrace();
    }
  }



  private void addSongToPlaylist(Account account, long playerId, long libId, boolean attemptReauth, Intent originalIntent){
    String authToken = "";
    AccountManager am = AccountManager.get(this);
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(AuthenticatorException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "Authentication exception when adding to playist");
    }
    catch(OperationCanceledException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "Op Canceled exception when adding to playist");
    }
    catch(IOException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "IO exception when geting authtoken for adding to playist");
      Log.e(TAG, e.getMessage());
    }
    
    try{
      ServerConnection.addSongToActivePlaylist(
          playerId, libId, authToken);
    }
    catch(JSONException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "JSON exception when adding to playist");
    }
    catch(ParseException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "Parse exception when adding to playist");
    }
    catch(IOException e){
      alertAddSongException(account, originalIntent);
      Log.e(TAG, "IO exception when adding to playist");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken); 
        addSongToPlaylist(account, playerId, libId, false, originalIntent);
        Log.e(TAG, "Soft Authentication exception when adding to playist");
      }
      else{
        alertAddSongException(account, originalIntent);
        Log.e(TAG, "Hard Authentication exception when adding to playist");
      }
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when retreiving playlist");
      Utils.handleInactivePlayer(this, account);
    } catch (PlayerAuthException e) {
      //TODO REAUTH AND THEN TRY ADD AGAIN
      e.printStackTrace();
    }
    
  }
  
  private void removeSongFromPlaylist(
      Account account, long playerId, long libId, boolean attemptReauth, Intent originalIntent)
  {
    String authToken = "";
    AccountManager am = AccountManager.get(this);
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(AuthenticatorException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "Authentication exception when removing from playist");
    }
    catch(OperationCanceledException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "Op Canceled exception when removing from playist");
    }
    catch(IOException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "IO exception when removing from playist/getting authtoken");
      Log.e(TAG, e.getMessage());
    }

    try{
      Log.d(TAG, "Actually removing song");
      ServerConnection.removeSongFromActivePlaylist(playerId, libId, authToken);
    }
    catch(ParseException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "Parse exception when removing from playist");
    }
    catch(IOException e){
      alertRemoveSongException(account, originalIntent);
      Log.e(TAG, "IO exception when removing from playist");
      Log.e(TAG, e.getMessage());
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken); 
        removeSongFromPlaylist(account, playerId, libId, false, originalIntent);
        Log.e(TAG, "Soft Authentication exception when removing from playist");
      }
      else{
        alertRemoveSongException(account, originalIntent);
        Log.e(TAG, "Hard Authentication exception when removing from playist");
      }
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when removing from playlist");
      Utils.handleInactivePlayer(this, account);
    } catch (PlayerAuthException e) {
      // TODO REAUTH AND THEN TRY AGAIN
      e.printStackTrace();
    }
    
  }

  private void voteOnSong(Account account, long playerId, long libId, int voteWeight, boolean attemptReauth){
    AccountManager am = AccountManager.get(this);
    String authToken = "";
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when voting on playist");
    }
    catch(AuthenticatorException e){
      Log.e(TAG, "Authentication exception when voting playist");
    }
    catch(OperationCanceledException e){
      Log.e(TAG, "Op Canceled exception when voting playist");
    }

    try{
      Long userId = 
          Long.valueOf(am.getUserData(account, Constants.USER_ID_DATA));
      ServerConnection.voteOnSong(playerId, userId, libId, voteWeight, authToken);
    }
    catch(ParseException e){
      Log.e(TAG, "Parse exception when retreiving playist");
    }
    catch(IOException e){
      Log.e(TAG, "IO exception when retreiving playist");
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        Log.e(TAG, "Soft Authentication exception when retreiving playist");
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken); 
        voteOnSong(account, playerId, libId, voteWeight, false);
      }
      else{
        Log.e(TAG, "Hard Authentication exception when retreiving playist");
      }
    }
    catch(PlayerInactiveException e){
      Log.e(TAG, "Event over exceptoin when retreiving playlist");
      Utils.handleInactivePlayer(this, account);
    } catch (PlayerAuthException e) {
      // TODO REAUTH AND THEN TRY AGAIN
      e.printStackTrace();
    }
  }


  private void alertAddSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_add_failed_title,
      R.string.song_add_failed_content,
      SONG_ADD_EXCEPTION_ID
    );
  }

  private void alertRemoveSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_remove_failed_title,
      R.string.song_remove_failed_content,
      SONG_REMOVE_EXCEPTION_ID
    );
  }

  private void alertSetSongException(Account account, Intent originalIntent){
    alertException(
      account,
      originalIntent,
      R.string.song_set_failed_title,
      R.string.song_set_failed_content,
      SONG_SET_EXCEPTION_ID);
  }

  private void alertException(Account account, Intent originalIntent,
    int titleRes, int contentRes, int notificationId)
  {

    PendingIntent pe = PendingIntent.getService(
      this, 0, originalIntent, 0);
    Notification notification = 
      new Notification(R.drawable.udjlauncher, "", System.currentTimeMillis());
    notification.setLatestEventInfo(
        this,
        getString(titleRes),
        getString(contentRes),
        pe);

    NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(notificationId, notification);
  }


}
