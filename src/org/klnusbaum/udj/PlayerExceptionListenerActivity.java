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

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.content.DialogInterface;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.util.Log;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.DialogFragment;

import org.klnusbaum.udj.network.PlayerCommService;

import com.actionbarsherlock.app.SherlockFragmentActivity;


public abstract class PlayerExceptionListenerActivity extends SherlockFragmentActivity{

  private static final String PLAYER_INACTIVE_DIALOG = "player_inactive_dialog";
  private static final String NO_LONGER_IN_PLAYER_DIALOG = "no_longer_in_player_dialog";
  private static final String KICKED_FROM_PLAYER_DIALOG = "kicked_from_player_dialog";
  private static final String TAG = "PlayerInactivityListenerActivity";
  protected Account account;

  private BroadcastReceiver playerInactivityReceiver = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved player went inactive broadcast");
      unregisterReceiver(playerInactivityReceiver);
      playerWentInactive();
    }
  };

  private BroadcastReceiver noLongerInPlayerReceiver = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved no longer in player broadcast");
      unregisterReceiver(noLongerInPlayerReceiver);
      noLongerInPlayer();
    }
  };

  private BroadcastReceiver kickedFromPlayerReceiver = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved no kicked from player broadcast");
      unregisterReceiver(kickedFromPlayerReceiver);
      kickedFromPlayer();
    }
  };

  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    account = Utils.basicGetUdjAccount(this);
  }

  @Override
  protected void onResume(){
    super.onResume();
    int playerState = Utils.getPlayerState(this, account);
    if(playerState == Constants.LEAVING_PLAYER || 
      playerState == Constants.NOT_IN_PLAYER)
    {
      setResult(Activity.RESULT_OK);
      finish();
    }
    else if(playerState == Constants.PLAYER_ENDED){
      playerWentInactive();
    }
    else if(playerState == Constants.NO_LONGER_IN_PLAYER){
      noLongerInPlayer();
    }
    else if(playerState == Constants.KICKED_FROM_PLAYER){
      kickedFromPlayer();
    }
    else{
      registerReceiver(
        playerInactivityReceiver,
        new IntentFilter(Constants.PLAYER_INACTIVE_ACTION));
      registerReceiver(
        noLongerInPlayerReceiver,
        new IntentFilter(Constants.NO_LONGER_IN_PLAYER_ACTION));
      registerReceiver(
        kickedFromPlayerReceiver,
        new IntentFilter(Constants.KICKED_FROM_PLAYER_ACTION));
    }
  }

  @Override
  protected void onPause(){
    super.onPause();
    try{
      unregisterReceiver(playerInactivityReceiver);
    }
    catch(IllegalArgumentException e){

    }
    try{
      unregisterReceiver(noLongerInPlayerReceiver);
    }
    catch(IllegalArgumentException e){

    }
    try{
      unregisterReceiver(kickedFromPlayerReceiver);
    }
    catch(IllegalArgumentException e){

    }
  }

  private void noLongerInPlayer(){
    DialogFragment newFrag = new PlayerExceptionDialog();
    Bundle args = new Bundle();
    args.putParcelable(Constants.ACCOUNT_EXTRA,account);
    args.putInt(PlayerExceptionDialog.DIALOG_TITLE, R.string.no_longer_in_player_title);
    args.putInt(PlayerExceptionDialog.DIALOG_MESSAGE, R.string.no_longer_in_player_message);
    newFrag.setArguments(args);
    newFrag.show(getSupportFragmentManager(), NO_LONGER_IN_PLAYER_DIALOG);
  }

  private void playerWentInactive(){
    DialogFragment newFrag = new PlayerExceptionDialog();
    Bundle args = new Bundle();
    args.putParcelable(Constants.ACCOUNT_EXTRA,account);
    args.putInt(PlayerExceptionDialog.DIALOG_TITLE, R.string.player_inactive_title);
    args.putInt(PlayerExceptionDialog.DIALOG_MESSAGE, R.string.player_inactive_message);
    newFrag.setArguments(args);
    newFrag.show(getSupportFragmentManager(), PLAYER_INACTIVE_DIALOG);
  }

  private void kickedFromPlayer(){
    DialogFragment newFrag = new PlayerExceptionDialog();
    Bundle args = new Bundle();
    args.putParcelable(Constants.ACCOUNT_EXTRA,account);
    args.putInt(PlayerExceptionDialog.DIALOG_TITLE, R.string.kicked_from_player_title);
    args.putInt(PlayerExceptionDialog.DIALOG_MESSAGE, R.string.kicked_from_player_message);
    newFrag.setArguments(args);
    newFrag.show(getSupportFragmentManager(), KICKED_FROM_PLAYER_DIALOG);
  }


  public static class PlayerExceptionDialog extends DialogFragment{

    public static final String DIALOG_TITLE = "dialog_title";
    public static final String DIALOG_MESSAGE = "dialog_message";
    private Account getAccount(){
      return (Account)getArguments().getParcelable(Constants.ACCOUNT_EXTRA);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      return new AlertDialog.Builder(getActivity())
        .setTitle(getArguments().getInt(DIALOG_TITLE))
        .setMessage(getArguments().getInt(DIALOG_MESSAGE))
        .setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
              finalizePlayer();
            }
          })
        .setOnCancelListener(new DialogInterface.OnCancelListener(){
          public void onCancel(DialogInterface dialog){
            finalizePlayer(); 
          }
        })
        .create();
    }
    
    private void finalizePlayer(){
      AccountManager am = AccountManager.get(getActivity());
      Utils.leavePlayer(am, getAccount());
      getActivity().setResult(Activity.RESULT_OK);
      getActivity().finish();
    }
  }

}
