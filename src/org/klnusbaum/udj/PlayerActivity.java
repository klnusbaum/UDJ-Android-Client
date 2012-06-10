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

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import android.os.Bundle;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.MenuInflater;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.SearchManager;


import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.network.PlaylistSyncService;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The main activity display class.
 */
public class PlayerActivity extends PlayerInactivityListenerActivity {
  private static final String TAG = "PlayerActivity";

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
    getPlaylistFromServer();

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

  public void getPlaylistFromServer() {
    int playerState = Utils.getPlayerState(this, account);
    // TODO hanle if no player
    if (playerState == Constants.IN_PLAYER) {
      Intent getPlaylist = new Intent(Intent.ACTION_VIEW,
          UDJPlayerProvider.PLAYLIST_URI, this,
          PlaylistSyncService.class);
      getPlaylist.putExtra(Constants.ACCOUNT_EXTRA, account);
      startService(getPlaylist);
    }
  }

  public void onBackPressed(){
      AccountManager am = AccountManager.get(this);
      Utils.leavePlayer(am, account);
      finish();
  }



  public static class PlayerPagerAdapter extends FragmentPagerAdapter implements TitleProvider{
    public PlayerPagerAdapter(FragmentManager fm){
      super(fm);
    }

    @Override
    public int getCount(){
      return 3;
    }

    public Fragment getItem(int position){
      switch(position){
        case 0:
          return new PlaylistFragment();
        case 1:
          return new ArtistsDisplayFragment();
        case 2:
          return new RandomSearchFragment();
        default:
          return null;
      }
    }

    public String getTitle(int position){
      switch(position){
        case 0:
          return "Playlist";
        case 1:
          return "Artists";
        case 2:
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
        menu.add("Pause")
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
      else if(playbackState == Constants.PAUSED_STATE){
        menu.add("Play")
          .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
    }
    menu.add("Search")
      .setIcon(R.drawable.ic_search)
      .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getTitle().equals("Search")){
      startSearch(null, false, null, false);
      return true;
    }
    else if(item.getTitle().equals("Pause")){
      setPlayback(Constants.PAUSED_STATE);
    }
    else if(item.getTitle().equals("Play")){
      setPlayback(Constants.PLAYING_STATE);
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

}
