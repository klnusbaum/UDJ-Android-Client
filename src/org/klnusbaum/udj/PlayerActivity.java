/**
 * Copyright 2011 Kurtis L. Nusbaum
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

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.app.DialogFragment;

import android.os.Bundle;
import android.widget.SeekBar;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.MenuInflater;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.SearchManager;
import android.widget.Toast;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;


import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.network.PlaylistSyncService;

import com.viewpagerindicator.TitlePageIndicator;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The main activity display class.
 */
public class PlayerActivity extends PlayerInactivityListenerActivity {
  private static final String TAG = "PlayerActivity";
  private static final String VOLUME_FRAGMENT_TAG = "VolumeFragment";

  private PlayerPagerAdapter pagerAdapter;
  private ViewPager pager;

  private BroadcastReceiver playbackStateChangedListener = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      Log.d(TAG, "Recieved playback changed broadcast");
      invalidateOptionsMenu();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.player);

    pagerAdapter = new PlayerPagerAdapter(getSupportFragmentManager());
    pager = (ViewPager)findViewById(R.id.player_pager);
    pager.setAdapter(pagerAdapter);

    TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
    titleIndicator.setViewPager(pager);
  }

  @Override
  protected void onResume(){
    super.onResume();
    registerReceiver(
      playbackStateChangedListener,
      new IntentFilter(Constants.BROADCAST_PLAYBACK_CHANGED)
    );
  }

  @Override
  protected void onPause(){
    super.onPause();
    unregisterReceiver(playbackStateChangedListener);
  }

  public void onBackPressed(){
      AccountManager am = AccountManager.get(this);
      Utils.leavePlayer(am, account);
      finish();
  }



  public static class PlayerPagerAdapter extends FragmentPagerAdapter{
    public PlayerPagerAdapter(FragmentManager fm){
      super(fm);
    }

    @Override
    public int getCount(){
      return 4;
    }

    public Fragment getItem(int position){
      switch(position){
        case 0:
          return new PlaylistFragment();
        case 1:
          return new ArtistsDisplayFragment();
        case 2:
          return new RecentlyPlayedFragment();
        case 3:
          return new RandomSearchFragment();
        default:
          return null;
      }
    }

    public String getPageTitle(int position){
      switch(position){
        case 0:
          return "Playlist";
        case 1:
          return "Artists";
        case 2:
          return "Recent";
        case 3:
          return "Random";
        default:
          return "Unknown";
      }
    }
  }

  public boolean onCreateOptionsMenu(Menu menu){
    AccountManager am = AccountManager.get(this);
    if(Utils.isCurrentPlayerOwner(am, account)){
      int playbackState = Utils.getPlaybackState(am, account);
      if(playbackState == Constants.PLAYING_STATE){
        menu.add(getString(R.string.pause))
          .setIcon(R.drawable.ab_pause)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      }
      else if(playbackState == Constants.PAUSED_STATE){
        menu.add(getString(R.string.play))
          .setIcon(R.drawable.ab_play)
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      }

      menu.add(getString(R.string.volume_set))
        .setIcon(R.drawable.ab_volume)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }
    menu.add(getString(R.string.search))
      .setIcon(R.drawable.ab_search_dark)
      .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getTitle().equals(getString(R.string.search))){
      startSearch(null, false, null, false);
      return true;
    }
    else if(item.getTitle().equals(getString(R.string.pause))){
      setPlayback(Constants.PAUSED_STATE);
    }
    else if(item.getTitle().equals(getString(R.string.play))){
      setPlayback(Constants.PLAYING_STATE);
    }
    else if(item.getTitle().equals(getString(R.string.volume_set))){
      SetVolumeFragment volumeFragment = new SetVolumeFragment();
      Bundle volumeArguments = new Bundle();
      volumeArguments.putParcelable(Constants.ACCOUNT_EXTRA, account);
      volumeFragment.setArguments(volumeArguments);
      volumeFragment.show(getSupportFragmentManager(), VOLUME_FRAGMENT_TAG);
    }
    return false;
  }

  private void setPlayback(int newPlaybackState){
    changePlaybackMenuOption(newPlaybackState);
    Intent setPlaybackIntent = new Intent(
      Constants.ACTION_SET_PLAYBACK,
      Constants.PLAYER_URI,
      this,
      PlaylistSyncService.class);
    setPlaybackIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    setPlaybackIntent.putExtra(Constants.PLAYBACK_STATE_EXTRA, newPlaybackState);
    startService(setPlaybackIntent);
  }

  private void changePlaybackMenuOption(int newPlaybackState){
    AccountManager am = AccountManager.get(this);
    am.setUserData(account, Constants.PLAYBACK_STATE_DATA, String.valueOf(newPlaybackState));
    invalidateOptionsMenu();
  }

  protected void onNewIntent(Intent intent){
    if(Intent.ACTION_SEARCH.equals(intent.getAction())){
      String searchQuery = intent.getStringExtra(SearchManager.QUERY);
      searchQuery = searchQuery.trim();
      intent.putExtra(SearchManager.QUERY, searchQuery);
      intent.setClass(this, RegularSearchActivity.class);
      startActivityForResult(intent, 0);
    }
    else{
      super.onNewIntent(intent);
    }
  }

  private void setVolume(int newVolume){
    PlayerActivity.setVolume(this, account, newVolume);
  }

  private static void setVolume(Context context, Account account, int newVolume){
    Intent setPlaybackIntent = new Intent(
      Constants.ACTION_SET_VOLUME,
      Constants.PLAYER_URI,
      context,
      PlaylistSyncService.class);
    setPlaybackIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    setPlaybackIntent.putExtra(Constants.PLAYER_VOLUME_EXTRA, newVolume);
    context.startService(setPlaybackIntent);
  }


  public static class SetVolumeFragment extends DialogFragment
    implements SeekBar.OnSeekBarChangeListener, DialogInterface.OnClickListener
  {
    private TextView volumeDisplay;
    private SeekBar volumeBar;

    private Account getAccount(){
      return (Account)getArguments().getParcelable(Constants.ACCOUNT_EXTRA);
    }

    public void onClick(DialogInterface dialog, int whichButton){
      int requestedVolume = volumeBar.getProgress();
      Toast toast = Toast.makeText(
          getActivity(),
          "Setting Volume To " + String.valueOf(requestedVolume), Toast.LENGTH_SHORT);
      toast.show();
      PlayerActivity.setVolume(getActivity(), getAccount(), requestedVolume);
      dismiss();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      AlertDialog toReturn = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.volume_set)
        .setPositiveButton(android.R.string.ok, this)
        .create();
      LayoutInflater inflater = getActivity().getLayoutInflater();
      View volumeEditor = inflater.inflate(R.layout.set_volume, null, false);
      toReturn.setView(volumeEditor);

      AccountManager am = AccountManager.get(getActivity());
      volumeBar = (SeekBar)volumeEditor.findViewById(R.id.volume_selector);
      volumeDisplay = (TextView)volumeEditor.findViewById(R.id.volume_display);
      volumeBar.setMax(10);
      volumeBar.setProgress(Utils.getPlayerVolume(am, getAccount()));
      volumeBar.setPadding(volumeBar.getThumbOffset()+2, 2, volumeBar.getThumbOffset()+2, 2);
      volumeDisplay.setText(String.valueOf(Utils.getPlayerVolume(am, getAccount())));
      volumeBar.setOnSeekBarChangeListener(this);

      return toReturn;
    }

    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser){
      volumeDisplay.setText(String.valueOf(progress));
    }

    public void onStartTrackingTouch(SeekBar seekbar){}
    public void onStopTrackingTouch(SeekBar seekbar){}
  }

}
