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

public abstract class PlayerInactivityListenerActivity extends FragmentActivity{

  private static final String PLAYER_INACTIVE_DIALOG = "player_inactive_dialog";
  private static final String TAG = "PlayerInactivityListenerActivity";
  protected Account account;

  private BroadcastReceiver playerInactivityReciever = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved player went inactive broadcast");
      unregisterReceiver(playerInactivityReciever);
      playerWentInactive();
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
    else{
      registerReceiver(
        playerInactivityReciever, 
        new IntentFilter(Constants.PLAYER_INACTIVE_ACTION));
    }
  }

  @Override
  protected void onPause(){
    super.onPause();
    try{
      unregisterReceiver(playerInactivityReciever);
    }
    catch(IllegalArgumentException e){

    }
  }

  private void playerWentInactive(){
    DialogFragment newFrag = new PlayerInactiveDialog();
    Bundle args = new Bundle();
    args.putParcelable(Constants.ACCOUNT_EXTRA,account);
    newFrag.setArguments(args);
    newFrag.show(getSupportFragmentManager(), PLAYER_INACTIVE_DIALOG);
  }

  public static class PlayerInactiveDialog extends DialogFragment{

    private Account getAccount(){
      return (Account)getArguments().getParcelable(Constants.ACCOUNT_EXTRA);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.player_inactive_title)
        .setMessage(R.string.player_inactive_message)
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
