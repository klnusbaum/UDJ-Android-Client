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


import android.content.ContentResolver;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.accounts.AuthenticatorException;
import android.util.Log;
import android.app.IntentService;
import android.content.Intent;

import java.io.IOException;

import org.json.JSONException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.Utils;
import org.klnusbaum.udj.UDJPlayerProvider;
import org.klnusbaum.udj.exceptions.PlayerInactiveException;
import org.klnusbaum.udj.exceptions.PlayerPasswordException;


/**
 * Adapter used to sync up with the UDJ server.
 */
public class PlayerCommService extends IntentService{

  public enum PlayerJoinError{
    NO_ERROR,
    AUTHENTICATION_ERROR,
    SERVER_ERROR,
    PLAYER_INACTIVE_ERROR,
    NO_NETWORK_ERROR,
    PLAYER_PASSWORD_ERROR,
    UNKNOWN_ERROR
  }

  private static final String TAG = "EventCommService";

  public PlayerCommService(){
    super("EventCommService");
  }

  @Override
  public void onHandleIntent(Intent intent){
    Log.d(TAG, "In Event Comm Service");
    AccountManager am = AccountManager.get(this);
    final Account account = 
      (Account)intent.getParcelableExtra(Constants.ACCOUNT_EXTRA);
    if(intent.getAction().equals(Intent.ACTION_INSERT)){
      enterEvent(intent, am, account, true);
    }
    else{
      Log.d(TAG, "ACTION wasn't delete or insert, it was " + 
        intent.getAction());
    } 
  }

  private void enterEvent(
  		Intent intent, AccountManager am, Account account, boolean attemptReauth)
  {
  	if(!Utils.isNetworkAvailable(this)){
  		doLoginFail(am, account, PlayerJoinError.NO_NETWORK_ERROR);
  		return;
  	}

  	long userId, eventId;
  	String authToken;
  	String password = "";
  	boolean hasPassword = false;
  	//TODO hanle error if account isn't provided
  	try{
  		userId = 
  				Long.valueOf(am.getUserData(account, Constants.USER_ID_DATA));
  		//TODO handle if event id isn't provided
  		authToken = am.blockingGetAuthToken(account, "", true);  
  		eventId = intent.getLongExtra(
  				Constants.PLAYER_ID_EXTRA,
  				Constants.NO_PLAYER_ID);
  		if(intent.hasExtra(Constants.PLAYER_PASSWORD_EXTRA)){
  			Log.d(TAG, "password given for event");
  			hasPassword = true;
  			password = intent.getStringExtra(Constants.PLAYER_PASSWORD_EXTRA);
  		}
  		else{
  			Log.d(TAG, "No password given for event");
  		}
  	}
  	catch(OperationCanceledException e){
  		Log.e(TAG, "Operation canceled exception in EventCommService" );
  		doLoginFail(am, account, PlayerJoinError.AUTHENTICATION_ERROR);
  		return;
  	}
  	catch(AuthenticatorException e){
  		Log.e(TAG, "Authenticator exception in EventCommService" );
  		doLoginFail(am, account, PlayerJoinError.AUTHENTICATION_ERROR);
  		return;
  	}
  	catch(IOException e){
  		Log.e(TAG, "IO exception in EventCommService" );
  		doLoginFail(am, account, PlayerJoinError.AUTHENTICATION_ERROR);
  		return;
  	}

  	try{
  		if(!hasPassword){
  			ServerConnection.joinPlayer(eventId, userId, authToken);
  		}
  		else{
  			ServerConnection.joinPlayer(eventId, userId, password, authToken);
  		}
  		setEventData(intent, am, account);
  		ContentResolver cr = getContentResolver();
  		UDJPlayerProvider.playerCleanup(cr);
  		Intent joinedEventIntent = new Intent(Constants.JOINED_PLAYER_ACTION);
  		am.setUserData(
  				account, Constants.LAST_PLAYER_ID_DATA, String.valueOf(eventId));
  		am.setUserData(
  				account, 
  				Constants.PLAYER_STATE_DATA, 
  				String.valueOf(Constants.IN_PLAYER));
  		Log.d(TAG, "Sending joined event broadcast");
  		sendBroadcast(joinedEventIntent);
  	}
  	catch(IOException e){
  		Log.e(TAG, "IO exception when joining event");
  		Log.e(TAG, e.getMessage());
  		doLoginFail(am, account, PlayerJoinError.SERVER_ERROR);
  	}
  	catch(JSONException e){
  		Log.e(TAG, 
  				"JSON exception when joining event");
  		Log.e(TAG, e.getMessage());
  		doLoginFail(am, account, PlayerJoinError.SERVER_ERROR);
  	}
  	catch(AuthenticationException e){
  		handleLoginAuthException(intent, am, account, authToken, attemptReauth);
  	}
  	catch(PlayerInactiveException e){
  		Log.e(TAG, "Player inactive Exception when joining player");
  		doLoginFail(am, account, PlayerJoinError.PLAYER_INACTIVE_ERROR);
  	} catch (ParseException e) {
  		e.printStackTrace();
  		doLoginFail(am, account, PlayerJoinError.SERVER_ERROR);
  	} catch (PlayerPasswordException e) {
  		Log.e(TAG, "Player Password Exception");
  		e.printStackTrace();
  		doLoginFail(am, account, PlayerJoinError.PLAYER_PASSWORD_ERROR);
  	}
  }

  private void handleLoginAuthException(
    Intent intent, AccountManager am, Account account, 
    String authToken, boolean attemptReauth)
  {
    if(attemptReauth){
      Log.d(TAG, 
        "Soft Authentication exception when joining event");
      am.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
      enterEvent(intent, am, account, false);
    }
    else{
      Log.e(TAG, 
        "Hard Authentication exception when joining event");
      doLoginFail(am, account, PlayerJoinError.AUTHENTICATION_ERROR);
    }
  }

  private void doLoginFail(
    AccountManager am, 
    Account account, 
    PlayerJoinError error)
  {
    am.setUserData(
      account,
      Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.PLAYER_JOIN_FAILED));
    am.setUserData(
      account, 
      Constants.PLAYER_JOIN_ERROR, 
      error.toString());
    Intent eventJoinFailedIntent = 
      new Intent(Constants.PLAYER_JOIN_FAILED_ACTION);
    Log.d(TAG, "Sending event join failure broadcast");
    sendBroadcast(eventJoinFailedIntent);
  }

  private void setEventData(Intent intent, AccountManager am, Account account){
    am.setUserData(
      account, 
      Constants.PLAYER_NAME_DATA, 
      intent.getStringExtra(Constants.PLAYER_NAME_EXTRA));
    am.setUserData(
      account, 
      Constants.PLAYER_HOSTNAME_DATA, 
      intent.getStringExtra(Constants.PLAYER_OWNER_EXTRA));
    am.setUserData(
      account, 
      Constants.PLAYER_HOST_ID_DATA, 
      String.valueOf(intent.getLongExtra(Constants.PLAYER_OWNER_ID_EXTRA,-1)));
    am.setUserData(
      account, 
      Constants.PLAYER_LAT_DATA, 
      String.valueOf(intent.getDoubleExtra(Constants.PLAYER_LAT_EXTRA, -100.0))
    );
    am.setUserData(
      account, 
      Constants.PLAYER_LONG_DATA, 
      String.valueOf(intent.getDoubleExtra(Constants.PLAYER_LONG_EXTRA, -100.0))
    );
  }
}
