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
import android.app.Dialog;
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
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
          .setIcon(R.drawable.ab_pause);
      }
      else if(playbackState == Constants.PAUSED_STATE){
        menu.add(getString(R.string.play))
          .setIcon(R.drawable.ab_play);
      }
      menu.add(getString(R.string.volume_set));
      menu.add(getString(R.string.volume_mute));
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
    else if(item.getTitle().equals(getString(R.string.volume_mute))){
      Toast toast = Toast.makeText(this,
        "Mute", Toast.LENGTH_SHORT);
      toast.show();
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


  private class SetVolumeFragment extends DialogFragment
      implements AdapterView.OnItemSelectedListener
  {
    private boolean isFirstSelect = true;

    private Account getAccount(){
      return (Account)getArguments().getParcelable(Constants.ACCOUNT_EXTRA);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
      Dialog toReturn = super.onCreateDialog(savedInstanceState);
      toReturn.setTitle(R.string.volume_set);
      return toReturn;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
      View v = inflater.inflate(R.layout.set_volume, container, false);
      Spinner volumeSelector = (Spinner)v.findViewById(R.id.volume_selector);
      ArrayAdapter<CharSequence> volumeAdapter = ArrayAdapter.createFromResource(getActivity(),
          R.array.volumes, android.R.layout.simple_spinner_item);
      volumeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      volumeSelector.setAdapter(volumeAdapter);
      AccountManager am = AccountManager.get(getActivity());
      volumeSelector.setSelection(Utils.getPlayerVolume(am, getAccount()));
      volumeSelector.setOnItemSelectedListener(this);
      return v;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
      if(isFirstSelect){
        isFirstSelect = false;
        return;
      }

      Toast toast = Toast.makeText(getActivity(),
        "Setting Volume To "+ String.valueOf(position), Toast.LENGTH_SHORT);
      toast.show();
      dismiss();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent){
      dismiss();
    }
  }

}
