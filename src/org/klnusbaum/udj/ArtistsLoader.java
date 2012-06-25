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

import android.content.Context;
import android.util.Log;
import android.accounts.OperationCanceledException;
import android.accounts.AuthenticatorException;
import android.accounts.AccountManager;
import android.accounts.Account;

import java.util.List;
import java.io.IOException;

import org.json.JSONException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.ParseException;

import org.klnusbaum.udj.exceptions.PlayerAuthException;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.network.ServerConnection;

public class ArtistsLoader
  extends AsyncTaskLoader<ArtistsLoader.ArtistsResult>
{

  public enum ArtistsError{
    NO_ERROR,
    PLAYER_INACTIVE_ERROR,
    SERVER_ERROR,
    AUTHENTICATION_ERROR,
    PLAYER_AUTH_ERROR
  };
  private static final String TAG = "ArtistsLoader";

  public static class ArtistsResult{
    public List<String> res;
    public ArtistsError error;

    public ArtistsResult(List<String> res){
      this.res = res;
      this.error = ArtistsError.NO_ERROR;
    }

    public ArtistsResult(List<String> res, ArtistsError error){
      this.res = res;
      this.error = error;
    }

  }

  private Account account;

  public ArtistsLoader(Context context, Account account){
    super(context);
    this.account = account;
  }

  public ArtistsResult loadInBackground(){

    return attemptLoad(true);
  }

  private ArtistsResult attemptLoad(boolean attemptReauth){
    AccountManager am = AccountManager.get(getContext());
    String authToken = "";
    try{
      authToken = am.blockingGetAuthToken(account, "", true);
    }
    catch(IOException e){
      //TODO this might actually be an auth error
      return new ArtistsResult(null, 
        ArtistsError.AUTHENTICATION_ERROR);
    }
    catch(AuthenticatorException e){
      return new ArtistsResult(null, 
        ArtistsError.AUTHENTICATION_ERROR);
    }
    catch(OperationCanceledException e){
      return new ArtistsResult(null, 
        ArtistsError.AUTHENTICATION_ERROR);
    }

    try{
      long playerId = 
        Long.valueOf(am.getUserData(account, Constants.LAST_PLAYER_ID_DATA));
      return getArtists(playerId, authToken);
    }
    catch(JSONException e){
      return new ArtistsResult(null, 
        ArtistsError.SERVER_ERROR);
    }
    catch(ParseException e){
      return new ArtistsResult(null, ArtistsError.SERVER_ERROR);
    }
    catch(IOException e){
      return new ArtistsResult(null, ArtistsError.SERVER_ERROR);
    }
    catch(AuthenticationException e){
      if(attemptReauth){
        Log.d(TAG, "soft auth failure");
        am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
        return attemptLoad(false);
      }
      else{
        Log.d(TAG, "hard auth failure");
        return new ArtistsResult(null, ArtistsError.AUTHENTICATION_ERROR);
      }
    }
    catch(PlayerInactiveException e){
      return new ArtistsResult(null, ArtistsError.PLAYER_INACTIVE_ERROR);
    } catch (PlayerAuthException e) {
      return new ArtistsResult(null, ArtistsError.PLAYER_AUTH_ERROR);
    }
  }

  @Override
  protected void onStartLoading(){
    forceLoad();
  }

  protected ArtistsResult getArtists(long playerId, String authToken) throws
    JSONException, ParseException, IOException, AuthenticationException,
    PlayerInactiveException, PlayerAuthException
  {
    List<String> list =
        ServerConnection.getArtists(playerId, authToken);
    return new ArtistsResult(list);
  }
}

