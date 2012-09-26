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

import org.klnusbaum.udj.PullToRefresh.RefreshableListFragment;
import org.klnusbaum.udj.network.PlaylistSyncService;
import org.klnusbaum.udj.containers.ActivePlaylistEntry;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


/**
 * Class used for displaying the contents of the Playlist.
 */
public class PlaylistFragment extends RefreshableListFragment implements
    LoaderManager.LoaderCallbacks<PlaylistLoader.PlaylistResult>
{

  private BroadcastReceiver playlistUpdateReceiver = new BroadcastReceiver(){
    public void onReceive(Context context, Intent intent){
      updatePlaylist();
    }
  };

  private static final String TAG = "PlaylistFragment";
  private static final int PLAYLIST_LOADER_ID = 0;
  private Account account;
  private AccountManager am;
  private String userId;
  /**
   * Adapter used to help display the contents of the playlist.
   */
  PlaylistAdapter playlistAdapter;

  @Override
  protected void doRefreshWork() {
    updatePlaylist();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    account = Utils.basicGetUdjAccount(getActivity());
    am = AccountManager.get(getActivity());
    userId = am.getUserData(account, Constants.USER_ID_DATA);
    setEmptyText(getActivity().getString(R.string.no_playlist_items));
    playlistAdapter = new PlaylistAdapter(getActivity(), null, this, userId, account);
    setListAdapter(playlistAdapter);
    setListShown(false);
    registerForContextMenu(getListView());
  }

  public void updatePlaylist() {
    getLoaderManager().restartLoader(PLAYLIST_LOADER_ID, null, this);
  }

  @Override
  public void onResume(){
    super.onResume();
    getLoaderManager().initLoader(PLAYLIST_LOADER_ID, null, this);
    IntentFilter updateFilters = new IntentFilter();
    updateFilters.addAction(Constants.BROADCAST_VOTE_COMPLETED);
    updateFilters.addAction(Constants.BROADCAST_SET_CURRENT_COMPLETE);
    updateFilters.addAction(Constants.BROADCAST_REMOVE_SONG_COMPLETE);
    getActivity().registerReceiver(playlistUpdateReceiver, updateFilters);
  }

  public void onPause(){
    super.onPause();
    try{
      getActivity().unregisterReceiver(playlistUpdateReceiver);
    }
    catch(IllegalArgumentException e){

    }
  }


  public void onListItemClick(ListView l, View v, int position, long id) {
    l.showContextMenuForChild(v);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){

    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    ActivePlaylistEntry playlistEntry = (ActivePlaylistEntry) playlistAdapter.getItem(info.position);
    MenuInflater inflater = getActivity().getMenuInflater();

    if(Utils.isCurrentPlayerOwner(am, account)){
      setupOwnerContext(playlistEntry.isCurrentSong, menu, inflater);
    }
    else{
      setupRegularContext(menu, inflater);
    }

    menu.setHeaderTitle(playlistEntry.song.getTitle());
  }

  private void setupOwnerContext(
      boolean isCurrentlyPlaying,
      ContextMenu menu,
      MenuInflater inflater)
  {
    inflater.inflate(R.menu.owner_playlist_context, menu);
    if(isCurrentlyPlaying){
      menu.findItem(R.id.set_current_song).setEnabled(false);
      menu.findItem(R.id.remove_song).setEnabled(false);
    }
  }

  private void setupRegularContext(ContextMenu menu, MenuInflater inflater){
    inflater.inflate(R.menu.playlist_context, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
        .getMenuInfo();
    switch (item.getItemId()) {
    case R.id.share:
      shareSong(info.position);
      return true;
    case R.id.remove_song:
      removeSong(info.position);
      return true;
    case R.id.set_current_song:
      setCurrentSong(info.position);
      return true;
    default:
      return super.onContextItemSelected(item);
    }
  }

  private void shareSong(int position) {
    ActivePlaylistEntry toShare = (ActivePlaylistEntry) playlistAdapter.getItem(position);
    String songTitle = toShare.song.getTitle();
    String playerName = am.getUserData(account, Constants.PLAYER_NAME_DATA);
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
        getString(R.string.song_share_1) + " " + songTitle + " "
            + getString(R.string.song_share_2) + " " + playerName
            + ".");
    startActivity(Intent.createChooser(shareIntent,
        getString(R.string.share_via)));

  }

  private void setCurrentSong(int position){
    ActivePlaylistEntry toSet = (ActivePlaylistEntry) playlistAdapter.getItem(position);
    Log.d(TAG, "Setting song with id " + toSet.song.getLibId());
    Intent setSongIntent = new Intent(
      Constants.ACTION_SET_CURRENT_SONG,
      Constants.PLAYLIST_URI,
      getActivity(),
      PlaylistSyncService.class);
    setSongIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    setSongIntent.putExtra(Constants.LIB_ID_EXTRA, toSet.song.getLibId());
    getActivity().startService(setSongIntent);
    playlistAdapter.setNewCurrentSong(toSet);
  }

  private void removeSong(int position) {
    ActivePlaylistEntry toRemove = (ActivePlaylistEntry) playlistAdapter.getItem(position);
    Log.d(TAG, "Removing song with id " + toRemove.song.getLibId());
    Intent removeSongIntent = new Intent(
      Intent.ACTION_DELETE,
      Constants.PLAYLIST_URI,
      getActivity(),
      PlaylistSyncService.class);
    removeSongIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    removeSongIntent.putExtra(Constants.LIB_ID_EXTRA, toRemove.song.getLibId());
    getActivity().startService(removeSongIntent);
    playlistAdapter.removeLibEntry(toRemove);
  }

  public Loader<PlaylistLoader.PlaylistResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
    case PLAYLIST_LOADER_ID:
      Log.d(TAG, "Starting playlist loader");
      return new PlaylistLoader(getActivity(), account);
    default:
      return null;
    }
  }

  public void onLoadFinished(
    Loader<PlaylistLoader.PlaylistResult> loader,
    PlaylistLoader.PlaylistResult data)
  {
    if (loader.getId() == PLAYLIST_LOADER_ID) {
      refreshDone();
      Log.d(TAG, "Playlist loader returned");
      if(data.error == PlaylistLoader.PlaylistLoadError.NO_ERROR){
        playlistAdapter.updatePlaylist(data.playlistEntries);
      }
      else if(data.error == PlaylistLoader.PlaylistLoadError.PLAYER_INACTIVE_ERROR){
        Utils.handleInactivePlayer(getActivity(), account);
      }
      else if(data.error == PlaylistLoader.PlaylistLoadError.NO_LONGER_IN_PLAYER_ERROR){
        Utils.handleNoLongerInPlayer(getActivity(), account);
      }
      else if(data.error == PlaylistLoader.PlaylistLoadError.KICKED_ERROR){
        Utils.handleKickedFromPlayer(getActivity(), account);
      }
      if (isResumed()) {
        setListShown(true);
      } else if (isVisible()) {
        setListShownNoAnimation(true);
      }
    }
  }

  public void onLoaderReset(Loader<PlaylistLoader.PlaylistResult> loader) {

  }

}
