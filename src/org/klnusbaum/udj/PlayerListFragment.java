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


import org.klnusbaum.udj.auth.AuthActivity;
import org.klnusbaum.udj.containers.Player;
import org.klnusbaum.udj.network.PlayerCommService;
import org.klnusbaum.udj.network.PlayerCommService.PlayerJoinError;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.handmark.pulltorefresh.extras.listfragment.PullToRefreshListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;


public class PlayerListFragment extends PullToRefreshListFragment implements 
  LoaderManager.LoaderCallbacks<PlayersLoader.PlayersLoaderResult>,
  LocationListener,
  OnRefreshListener<ListView>
{
  public enum SearchType{
    LOCATION_SEARCH,
    NAME_SEARCH
  }

  private static final String TAG = "PlayerListFragment";
  private static final String PROG_DIALOG_TAG = "prog_dialog";
  private static final String PLAYER_JOIN_FAIL_TAG = "prog_dialog";
  private static final String PASSWORD_TAG = "password_dialog";
  public static final String PLAYER_SEARCH_QUERY = 
    "org.klnusbaum.udj.PlayerSearchQuery";
  public static final String PLAYER_SEARCH_TYPE_EXTRA = 
    "org.klnusbaum.udj.PlayerSearchType";
  private static final int ACCOUNT_CREATION_REQUEST_CODE = 0;



  private PlayerListAdapter playerAdapter;
  private LocationManager lm;
  private Location lastKnown = null;
  private Account account = null;
  private AccountManager am;
  private SearchType searchType;
  private String nameQuery;

  private BroadcastReceiver playerJoinedReceiver = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved player broadcats");
      dismissProgress();
      getActivity().unregisterReceiver(playerJoinedReceiver);
      if(intent.getAction().equals(Constants.JOINED_PLAYER_ACTION)){
        Intent playerActivityIntent = new Intent(context, PlayerActivity.class);
        startActivity(playerActivityIntent); 
      }
      else if(intent.getAction().equals(Constants.PLAYER_JOIN_FAILED_ACTION)){
        refreshList();
        handlePlayerJoinFail();
      }
    }
  };

  public void onCreate(Bundle icicle){
    super.onCreate(icicle);
    setHasOptionsMenu(true);
  }

  public void onActivityCreated(Bundle icicle){
    super.onActivityCreated(icicle);
    am = AccountManager.get(getActivity());
    Account[] udjAccounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
    Log.d(TAG, "Accounts length was " + udjAccounts.length);
    if(udjAccounts.length < 1){
      Intent getAccountIntent = new Intent(getActivity(), AuthActivity.class);
      startActivityForResult(getAccountIntent, ACCOUNT_CREATION_REQUEST_CODE);
      return;
    }
    else if(udjAccounts.length == 1){
      account=udjAccounts[0];
    }
    else{
      account=udjAccounts[0];
      //TODO implement if there are more than 1 account
    }

    searchType = (SearchType)getArguments().getSerializable(PLAYER_SEARCH_TYPE_EXTRA);
    if(searchType == SearchType.NAME_SEARCH){
      nameQuery = getArguments().getString(PLAYER_SEARCH_QUERY);
    }


    setEmptyText(getActivity().getString(R.string.no_player_items));

    getPullToRefreshListView().setOnRefreshListener(this);

    playerAdapter = new PlayerListAdapter(getActivity());
    getPullToRefreshListView().getRefreshableView().setAdapter(playerAdapter);
    setListShown(false);

  }

  public void onStart(){
    super.onStart();

    if(searchType == SearchType.LOCATION_SEARCH){
      lm = (LocationManager)getActivity().getSystemService(
        Context.LOCATION_SERVICE);
      List<String> providers = lm.getProviders(false);
      if(providers.contains(LocationManager.GPS_PROVIDER)){
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 50, this);
        if(lastKnown == null){
          lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
      }
      if(providers.contains(LocationManager.NETWORK_PROVIDER)){
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0, 50, this);
        if(lastKnown == null){
          lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
      }
    }
  }

  public void onActivityResult(
    final int requestCode, final int resultCode, final Intent data)
  {
    switch(requestCode){
    case ACCOUNT_CREATION_REQUEST_CODE:
      if(resultCode == Activity.RESULT_OK){
        account = (Account)data.getParcelableExtra(Constants.ACCOUNT_EXTRA);
      }
      else{
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
      }
      break;
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }

  }

  public void onResume(){
    super.onResume();
    PlayerPasswordFragment pFrag =
      (PlayerPasswordFragment)getActivity().getSupportFragmentManager().findFragmentByTag(PASSWORD_TAG);
    if(pFrag != null){
      pFrag.registerPasswordEnteredListener(this);
    }

    if(account != null){
      int playerState = Utils.getPlayerState(getActivity(), account);
      Log.d(TAG, "Checking Player State");
      if(playerState == Constants.JOINING_PLAYER){
        Log.d(TAG, "Is joining");
        Log.d(TAG, "Reregistering player listener");
        registerPlayerListener();
      }
      else if(playerState == Constants.PLAYER_JOIN_FAILED){
        Log.d(TAG, "Player joining Failed");
        dismissProgress();
        handlePlayerJoinFail();
      }
      else if(playerState == Constants.IN_PLAYER){
        Log.d(TAG, "Already signed into player. Checking Progress visibility");
        if(isShowingProgress()){
          Log.d(TAG, "Determined progress is indeed showing");
          dismissProgress();
        }
        Intent startPlayerActivity = 
          new Intent(getActivity(), PlayerActivity.class);
        startActivity(startPlayerActivity);
        return;
      }

      if(playerAdapter == null || playerAdapter.getCount() ==0){
        refreshList();
      }
    }
  }

  public void onPause(){
    super.onPause();
    if(account != null){
      int playerState = Utils.getPlayerState(getActivity(), account);
      if(playerState == Constants.JOINING_PLAYER){
        getActivity().unregisterReceiver(playerJoinedReceiver);
      }
    }

    PlayerPasswordFragment pFrag = (PlayerPasswordFragment)getActivity().getSupportFragmentManager().findFragmentByTag(PASSWORD_TAG);
    if(pFrag != null){
      pFrag.unregisterPasswordEnteredListener();
    }
  }

  public void onStop(){
    super.onStop();
    if(searchType == SearchType.LOCATION_SEARCH){
      lm.removeUpdates(this);
    }
  }

  public void onLocationChanged(Location location){
    lastKnown = location;
  }

  public void onProviderDisabled(String provider){}
  public void onProviderEnabled(String provider){}
  public void onStatusChanged(String provider, int status, Bundle extras){}

  @Override
  public void onListItemClick(ListView l, View v, int position, long id){
    Player toJoin = (Player)l.getItemAtPosition(position);
    if(toJoin.getHasPassword()){
      Log.d(TAG, "Joining password player with id: " + toJoin.getId());
      getPasswordForPlayer(toJoin);
    }
    else{
      Log.d(TAG, "Joining regular player with id: " + toJoin.getId());
      joinPlayer(toJoin);
    }
  }

  public void getPasswordForPlayer(Player toJoin){
    Bundle playerBundle = toJoin.bundleUp();
    PlayerPasswordFragment passwordFragment = new PlayerPasswordFragment();
    passwordFragment.registerPasswordEnteredListener(this);
    passwordFragment.setArguments(playerBundle);
    passwordFragment.show(getActivity().getSupportFragmentManager(), PASSWORD_TAG);
  }

  public void joinPlayer(Player toJoin){
    joinPlayer(toJoin, "");
  }

  public void joinPlayer(Player toJoin, String password){
    Log.d(TAG, "Joining Player with id: " + toJoin.getId());
    am.setUserData(
      account,
      Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.JOINING_PLAYER));
    showProgress();
    Intent joinPlayerIntent = new Intent(
      Intent.ACTION_INSERT,
      Constants.PLAYER_URI,
      getActivity(),
      PlayerCommService.class);
    joinPlayerIntent.putExtra(
      Constants.PLAYER_ID_EXTRA,
      toJoin.getId());
    joinPlayerIntent.putExtra(
      Constants.PLAYER_NAME_EXTRA,
      toJoin.getName());
    joinPlayerIntent.putExtra(
      Constants.PLAYER_OWNER_EXTRA,
      toJoin.getOwner().getUsername());
    joinPlayerIntent.putExtra(
      Constants.PLAYER_OWNER_ID_EXTRA,
      toJoin.getOwner().getId());
    joinPlayerIntent.putExtra(
      Constants.PLAYER_LAT_EXTRA,
      toJoin.getLatitude());
    joinPlayerIntent.putExtra(
      Constants.PLAYER_LONG_EXTRA,
      toJoin.getLongitude());
    joinPlayerIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    if(!password.equals("")){
      joinPlayerIntent.putExtra(Constants.PLAYER_PASSWORD_EXTRA, password);
    }
    getActivity().startService(joinPlayerIntent);
  }

  public Loader<PlayersLoader.PlayersLoaderResult> onCreateLoader(
    int id, Bundle args)
  {
    if(searchType == SearchType.LOCATION_SEARCH){
      return new PlayersLoader(
        getActivity(), 
        account,
        lastKnown);
    }
    else if(searchType == SearchType.NAME_SEARCH){
      return new PlayersLoader(
        getActivity(), 
        account,
        nameQuery);
    }
    else{
      return null;
    }
  }

  public void onLoadFinished(Loader<PlayersLoader.PlayersLoaderResult> loader, 
    PlayersLoader.PlayersLoaderResult data)
  {
    Log.d(TAG, "Got player search results");
    getPullToRefreshListView().onRefreshComplete();
    switch(data.getError()){
    case NO_ERROR:
      playerAdapter.updateList(data.getPlayers());
      break;
    case NO_LOCATION:
      setEmptyText(getString(R.string.no_location_error));
      break;
    case SERVER_ERROR:
      setEmptyText(getString(R.string.players_load_error));
      break;
    case NO_CONNECTION:
      setEmptyText(getString(R.string.no_network_connection));
      break;
    case NO_ACCOUNT:
      setEmptyText(getString(R.string.no_account_error));
      break;
    }

    if(isResumed()){
      setListShown(true);
    }
    else if(isVisible()){
      setListShownNoAnimation(true);
    }
  }

  public void onLoaderReset(Loader<PlayersLoader.PlayersLoaderResult> loader){
  }

  public void refreshList(){
    getLoaderManager().restartLoader(0, null, this);
  }

  @Override
  public void onRefresh(PullToRefreshBase<ListView> listView){
    refreshList();
  }

  private void showProgress(){
    registerPlayerListener();
    ProgressFragment progFragment = new ProgressFragment();
    progFragment.show(
      getActivity().getSupportFragmentManager(), PROG_DIALOG_TAG);
  }

  private void registerPlayerListener(){
    getActivity().registerReceiver(
      playerJoinedReceiver, 
      new IntentFilter(Constants.JOINED_PLAYER_ACTION));
    getActivity().registerReceiver(
      playerJoinedReceiver, 
      new IntentFilter(Constants.PLAYER_JOIN_FAILED_ACTION));
    Log.d(TAG, "Listener registered");
  }

  private boolean isShowingProgress(){
    ProgressFragment pd =
      (ProgressFragment)getActivity().getSupportFragmentManager().findFragmentByTag(PROG_DIALOG_TAG);
    return pd != null && pd.getDialog().isShowing();
  }

  private void dismissProgress(){
    ProgressFragment pd = (ProgressFragment)getActivity().getSupportFragmentManager().findFragmentByTag(PROG_DIALOG_TAG);
    //Sometimes the progress dialog is already dimissed. Need to check if it's null
    if(pd != null){
      pd.dismiss();
    }
  }

  public static class ProgressFragment extends DialogFragment{


    public Dialog onCreateDialog(Bundle icicle){
      //Stop gap for now. This should be cancelable
      setCancelable(false);
      final ProgressDialog dialog = new ProgressDialog(getActivity());
      dialog.setMessage(getActivity().getString(R.string.joining_player));
      dialog.setIndeterminate(true);
      return dialog;
    }
  }


  private void handlePlayerJoinFail(){
    PlayerJoinError joinError = PlayerJoinError.valueOf(
      am.getUserData(account, Constants.PLAYER_JOIN_ERROR));
    am.setUserData(
      account,
      Constants.PLAYER_STATE_DATA,
      String.valueOf(Constants.NOT_IN_PLAYER));
    DialogFragment newFrag = new PlayerJoinFailDialog();
    Bundle args = new Bundle();
    args.putInt(Constants.PLAYER_JOIN_ERROR_EXTRA, joinError.ordinal());
    newFrag.setArguments(args);
    newFrag.show(
      getActivity().getSupportFragmentManager(), PLAYER_JOIN_FAIL_TAG);
  }

  public static class PlayerJoinFailDialog extends DialogFragment{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      Bundle args = getArguments();
      PlayerJoinError joinError = 
        PlayerJoinError.values()[args.getInt(Constants.PLAYER_JOIN_ERROR_EXTRA)];
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.player_join_fail_title);
      String message; 
      switch(joinError){
      case SERVER_ERROR:
        message = getString(R.string.server_join_fail_message); 
        break;
      case AUTHENTICATION_ERROR:
        message = getString(R.string.auth_join_fail_message); 
        break;
      case PLAYER_INACTIVE_ERROR:
        message = getString(R.string.player_inactive_join_fail_message); 
        break;
      case NO_NETWORK_ERROR:
        message = getString(R.string.no_network_join_fail_message); 
        break;
      case PLAYER_PASSWORD_ERROR:
        message = getString(R.string.bad_player_password);
        break;
      case PLAYER_FULL_ERROR:
        message = getString(R.string.player_full);
        break;
      case BANNED_ERROR:
        message = getString(R.string.banned_from_player);
        break;
      default:
        message = getString(R.string.unknown_error_message);
      }
      return builder
        .setMessage(message)
        .setPositiveButton(
          android.R.string.ok,
          new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
              dismiss();
            }
          })
        .create();
    }
  }

  public static class PlayerPasswordFragment extends DialogFragment{

    Player toJoin;
    EditText passwordEdit;
    Button okButton;
    PlayerListFragment playerListFragment = null;

    public void onCreate(Bundle icicle){
      super.onCreate(icicle);
      toJoin = Player.unbundle(getArguments());
      Log.d(TAG, "Player id in PasswordFragment: " + toJoin.getId());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle){
        View v = inflater.inflate(R.layout.player_password, container, false);
        passwordEdit = (EditText)v.findViewById(R.id.player_password_edit);
        okButton = (Button)v.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener(){
          public void onClick(View v){
            passwordEntered();
          }
        });
        getDialog().setTitle(R.string.password_required);
        return v;
    }

    public void passwordEntered(){
      //TODO handle if they didn't type anything in
      playerListFragment.joinPlayer(toJoin, passwordEdit.getText().toString());
      dismiss();
    }

    public void registerPasswordEnteredListener(PlayerListFragment playerListFragment){
      this.playerListFragment = playerListFragment;
    }

    public void unregisterPasswordEnteredListener(){
      this.playerListFragment = null;
    }
  }
}
