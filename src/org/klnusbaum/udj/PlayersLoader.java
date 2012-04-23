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

package org.klnusbaum.udj;


import android.support.v4.content.AsyncTaskLoader;

import android.location.Location;
import android.util.Log;
import android.content.Context;
import android.accounts.OperationCanceledException;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;

import java.io.IOException;
import java.util.List;

import org.apache.http.auth.AuthenticationException;

import org.json.JSONException;

import org.klnusbaum.udj.network.ServerConnection;
import org.klnusbaum.udj.containers.Player;

public class PlayersLoader extends 
  AsyncTaskLoader<PlayersLoader.PlayersLoaderResult>
{
  public enum PlayerLoaderError{
    NO_ERROR, NO_CONNECTION, SERVER_ERROR, NO_LOCATION, 
    AUTHENTICATION_ERROR, NO_ACCOUNT};

  public static class PlayersLoaderResult{
    private List<Player> players;
    private PlayerLoaderError error; 
    public PlayersLoaderResult(List<Player> players, PlayerLoaderError error){
      this.players = players;
      this.error = error;
    }

    public PlayerLoaderError getError(){ 
      return error;
    }

    public List<Player> getPlayers(){
      return players;
    }
  }

  private static final String TAG = "PLAYERS_LOADER";

  private AccountManager am;
  private Account account;
  private Location location;
  private String searchQuery;
  private List<Player> players;
  private boolean locationSearch;

    
  public PlayersLoader(Context context, Account account, Location location){
    super(context);
    am = AccountManager.get(context);
    this.account = account;
    this.location = location;
    this.players = null;
    this.searchQuery = null;
    locationSearch = true;
  }

  public PlayersLoader(Context context, Account account, String query){
    super(context);
    am = AccountManager.get(context);
    this.account = account;
    this.location = null;
    this.players = null;
    this.searchQuery = query;
    locationSearch = false;
  }

  @Override
  protected void onStartLoading(){
    if(takeContentChanged() || players==null){
      forceLoad();
    }
  }
 
  public PlayersLoaderResult loadInBackground(){
    if(account == null){
      return new PlayersLoaderResult(null, PlayerLoaderError.NO_ACCOUNT);
    }
    else if(location == null && locationSearch){
      return new PlayersLoaderResult(null, PlayerLoaderError.NO_LOCATION);
    }
    else if(!Utils.isNetworkAvailable(getContext())){
      return new PlayersLoaderResult(null, PlayerLoaderError.NO_CONNECTION);
    }
    else{
      return doSearch(true);
    }
  }

  private PlayersLoaderResult doSearch(boolean attemptReauth){
    String authToken = "";
    try{
      authToken = am.blockingGetAuthToken(account, "", true); 
      if(locationSearch){
        Log.d(TAG, "Doing location search");
        return doLocationSearch(authToken);
      }
      else{
        Log.d(TAG, "Doing name search");
        return doNameSearch(authToken);
      }
    }
    catch(IOException e){
      Log.e(TAG, "IO exception");
      Log.d(TAG, e.getMessage());
    }
    catch(AuthenticatorException e){
      Log.e(TAG, "Authenticator exception");
      //TODO notify the user
    }
    catch(OperationCanceledException e){
      Log.e(TAG, "Operation cancelced exception");
      //TODO notify user
    }
    catch(JSONException e){
      Log.e(TAG, "Json exception");
      Log.e(TAG, e.getMessage());
      //TODO notify the user
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        Log.e(TAG, "Soft auth fail");
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
        return doSearch(false);
      }
      else{
        //TODO notify user
        Log.e(TAG, "Hard auth fail");
      }
    }
    return new PlayersLoaderResult(
      null, PlayerLoaderError.AUTHENTICATION_ERROR);
  }
        
  private PlayersLoaderResult doLocationSearch(String authToken)
    throws AuthenticationException, JSONException, IOException
  {
    List<Player> players = 
      ServerConnection.getNearbyPlayers(location, authToken);
    return new PlayersLoaderResult(players, PlayerLoaderError.NO_ERROR);
  }

  private PlayersLoaderResult doNameSearch(String authToken)
    throws AuthenticationException, JSONException, IOException
  {
    List<Player> players = 
      ServerConnection.searchForPlayers(searchQuery, authToken);
    return new PlayersLoaderResult(players, PlayerLoaderError.NO_ERROR);
  }
}
