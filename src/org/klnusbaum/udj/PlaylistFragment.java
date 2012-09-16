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
import org.klnusbaum.udj.containers.User;

import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
//import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
//import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.List;

/**
 * Class used for displaying the contents of the Playlist.
 */
public class PlaylistFragment extends RefreshableListFragment implements
    LoaderManager.LoaderCallbacks<PlaylistLoader.PlaylistResult> {
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
    //getLoaderManager().initLoader(PLAYLIST_LOADER_ID, null, this);
    registerForContextMenu(getListView());
  }

  public void updatePlaylist() {
    getLoaderManager().restartLoader(PLAYLIST_LOADER_ID, null, this);
    /*
    int playerState = Utils.getPlayerState(getActivity(), account);
    // TODO hanle if no player
    if (playerState == Constants.IN_PLAYER) {
      Intent getPlaylist = new Intent(Intent.ACTION_VIEW,
          UDJPlayerProvider.PLAYLIST_URI, getActivity(),
          PlaylistSyncService.class);
      getPlaylist.putExtra(Constants.ACCOUNT_EXTRA, account);
      getActivity().startService(getPlaylist);
    }
    */
  }

  @Override
  public void onResume(){
    super.onResume();
    updatePlaylist();
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
      UDJPlayerProvider.PLAYLIST_URI,
      getActivity(),
      PlaylistSyncService.class);
    setSongIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    setSongIntent.putExtra(Constants.LIB_ID_EXTRA, toSet.song.getLibId());
    getActivity().startService(setSongIntent);
    Toast toast = Toast.makeText(getActivity(),
        getString(R.string.setting_song), Toast.LENGTH_SHORT);
    toast.show();
  }

  private void removeSong(int position) {
    ActivePlaylistEntry toRemove = (ActivePlaylistEntry) playlistAdapter.getItem(position);
    Log.d(TAG, "Removing song with id " + toRemove.song.getLibId());
    Intent removeSongIntent = new Intent(
      Intent.ACTION_DELETE,
      UDJPlayerProvider.PLAYLIST_URI,
      getActivity(),
      PlaylistSyncService.class);
    removeSongIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    removeSongIntent.putExtra(Constants.LIB_ID_EXTRA, toRemove.song.getLibId());
    getActivity().startService(removeSongIntent);
    Toast toast = Toast.makeText(getActivity(),
        getString(R.string.removing_song), Toast.LENGTH_SHORT);
    toast.show();
  }

  public Loader<PlaylistLoader.PlaylistResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
    case PLAYLIST_LOADER_ID:
      /*
      Uri playlistUri = UDJPlayerProvider.PLAYLIST_URI.buildUpon().appendQueryParameter(
          UDJPlayerProvider.USER_ID_PARAM, userId).build();
      return new CursorLoader(getActivity(),
          playlistUri, null, null, null,
          UDJPlayerProvider.PRIORITY_COLUMN);
      */
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
      if(data.error == PlaylistLoader.PlaylistLoadError.NO_ERROR){
        playlistAdapter.updatePlaylist(data.playlistEntries);
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

  private static class PlaylistAdapter extends BaseAdapter{
    private static final String PLAYLIST_ADAPTER_TAG = "PlaylistAdapter";
    private static final int CURRENT_SONG_VIEW_TYPE = 0;
    private static final int REGULAR_SONG_VIEW_TYPE = 1;
    private List<ActivePlaylistEntry> playlist;
    private String userId;
    private Context context;
    private final PlaylistFragment plFrag;
    private Account account;
    private User me;

    public PlaylistAdapter(
      Context context,
      List<ActivePlaylistEntry> playlist,
      PlaylistFragment plFrag,
      String userId,
      Account account)
    {
      super();
      this.playlist = playlist;
      this.userId = userId;
      this.context = context;
      this.plFrag = plFrag;
      this.account = account;
      this.me = new User(userId);
    }

    public int getCount(){
      if(playlist != null){
        return playlist.size();
      }
      return 0;
    }

    public Object getItem(int position){
      if(playlist != null){
        return playlist.get(position);
      }
      return null;
    }

    public long getItemId(int position){
      return position;
    }

    public int getViewTypeCount(){
      return 2;
    }

    public boolean isEmpty(){
      return playlist == null || playlist.isEmpty();
    }

    public int getItemViewType(int position){
      if(playlist != null && playlist.get(position).isCurrentSong){
        return CURRENT_SONG_VIEW_TYPE;
      }
      return REGULAR_SONG_VIEW_TYPE;
    }

    public void updatePlaylist(List<ActivePlaylistEntry> newPlaylist){
      this.playlist = newPlaylist;
      notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ActivePlaylistEntry currentEntry = (ActivePlaylistEntry)getItem(position);
      final String libId = currentEntry.song.getLibId();
      View view;
      switch(getItemViewType(position)){
        case CURRENT_SONG_VIEW_TYPE:
          view = getCurrentSongView(convertView, parent);
          break;
        default:
          view = getRegularSongView(currentEntry, convertView, parent);
      }

      final TextView songName = (TextView) view.findViewById(R.id.playlistSongName);
      final String title = currentEntry.song.getTitle();
      songName.setText(title);

      final TextView artist = (TextView) view.findViewById(R.id.playlistArtistName);
      artist.setText(context.getString(R.string.by) + " " + currentEntry.song.getArtist());

      final TextView addByUser = (TextView) view.findViewById(R.id.playlistAddedBy);
      if (currentEntry.adder.equals(me)) {
        addByUser.setText(context.getString(R.string.added_by) 
            + " " + context.getString(R.string.you));
      }
      else{
        addByUser.setText(context.getString(R.string.added_by) + " " + currentEntry.adder.username);
      }

      final TextView upCount = (TextView) view.findViewById(R.id.upcount);
      final TextView downCount = (TextView) view.findViewById(R.id.downcount);
      upCount.setText(currentEntry.upvoters.size());
      downCount.setText(currentEntry.downvoters.size());

      view.setOnLongClickListener(new View.OnLongClickListener(){
        public boolean onLongClick(View v){
          plFrag.getListView().showContextMenuForChild(v);
          return true;
        }
      });

      return view;

    }

    private View getCurrentSongView(View convertView, ViewGroup parent){
      View view = convertView;
      if(convertView == null){
         LayoutInflater inflater = (LayoutInflater) context
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         view = inflater.inflate(R.layout.playlist_currentsong_item, null);
      }
      return view;
    }


    private View getRegularSongView(
      ActivePlaylistEntry currentEntry, View convertView, ViewGroup parent)
    {
      View view = convertView;
      if(convertView == null){
         LayoutInflater inflater = (LayoutInflater) context
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         view = inflater.inflate(R.layout.playlist_list_item, null);
      }
      final String libId = currentEntry.song.getLibId();
      final String title = currentEntry.song.getTitle();
      final ImageButton upButton = (ImageButton)view.findViewById(R.id.upvote_button);
      final ImageButton downButton = (ImageButton)view.findViewById(R.id.downvote_button);
      final Toast upVoteToast = Toast.makeText(context,
            context.getString(R.string.voting_up_message)+ " " + title, Toast.LENGTH_SHORT);
      upButton.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
          v.setEnabled(false);
          upVoteSong(libId);
          upVoteToast.show();
        }
      });

      final Toast downVoteToast = Toast.makeText(context,
            context.getString(R.string.voting_down_message)+ " " + title, Toast.LENGTH_SHORT);
      downButton.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
          v.setEnabled(false);
          downVoteSong(libId);
          downVoteToast.show();
        }
      });

      if(currentEntry.upvoters.contains(me)){
        upButton.setEnabled(false);
      }
      else if(currentEntry.downvoters.contains(me)){
        downButton.setEnabled(false);
      }

      return view;
    }

    /*

      ActivePlaylistEntry currentEntry = playlist.get(position);
      final String libId = currentEntry.song.getLibId();
      final boolean isCurrentlyPlaying = currentEntry.isCurrentlyPlaying;

      final View nowPlayingStuff = view.findViewById(R.id.nowplaying_stuff);
      final View upvoteStuff = view.findViewById(R.id.upvote_stuff);
      final View downvoteStuff = view.findViewById(R.id.downvote_stuff);
      final View songInfo = view.findViewById(R.id.song_info);
      final RelativeLayout.LayoutParams songInfoParams =
        new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams)songInfo.getLayoutParams());

      if(isCurrentlyPlaying){
        songInfoParams.addRule(0, R.id.nowplaying_stuff);
        ((RelativeLayout)view).updateViewLayout(songInfo, songInfoParams);
        nowPlayingStuff.setVisibility(View.VISIBLE);
        downvoteStuff.setVisibility(View.GONE);
        upvoteStuff.setVisibility(View.GONE);
        final TextView upCount = (TextView) view.findViewById(R.id.nowplaying_upcount);
        final TextView downCount = (TextView) view.findViewById(R.id.nowplaying_downcount);
        upCount.setText(cursor.isNull(upcountIndex) ? "0" : cursor.getString(upcountIndex));
        downCount.setText(cursor.isNull(downcountIndex) ? "0" : cursor.getString(downcountIndex));
      }
      else{
        songInfoParams.addRule(0, R.id.downvote_stuff);
        ((RelativeLayout)view).updateViewLayout(songInfo, songInfoParams);
        nowPlayingStuff.setVisibility(View.GONE);
        downvoteStuff.setVisibility(View.VISIBLE);
        upvoteStuff.setVisibility(View.VISIBLE);
        final TextView upCount = (TextView) view.findViewById(R.id.upcount);
        final TextView downCount = (TextView) view.findViewById(R.id.downcount);
        upCount.setText(cursor.isNull(upcountIndex) ? "0" : cursor.getString(upcountIndex));
        downCount.setText(cursor.isNull(downcountIndex) ? "0" : cursor.getString(downcountIndex));
      }

      view.setOnLongClickListener(new View.OnLongClickListener(){
        public boolean onLongClick(View v){
          plFrag.getListView().showContextMenuForChild(v);
          return true;
        }
      });

      //Vote button reset
      final ImageButton upButton = (ImageButton)view.findViewById(R.id.upvote_button);
      final ImageButton downButton = (ImageButton)view.findViewById(R.id.downvote_button);
      upButton.setVisibility(View.VISIBLE);
      downButton.setVisibility(View.VISIBLE);
      upButton.setEnabled(true);
      downButton.setEnabled(true);

      final TextView songName = (TextView) view.findViewById(R.id.playlistSongName);
      final String title = currentEntry.song.getTitle();
      songName.setText(title);

      final TextView artist = (TextView) view.findViewById(R.id.playlistArtistName);
      artist.setText(context.getString(R.string.by) + " " + currentEntry.song.getArtist());

      final TextView addByUser = (TextView) view.findViewById(R.id.playlistAddedBy);
      if (currentEntry.adder.equals(me)) {
        addByUser.setText(context.getString(R.string.added_by) + " " + context.getString(R.string.you));
      }
      else{
        addByUser.setText(context.getString(R.string.added_by) + " " + currentEntry.adder.username);
      }

      final Toast upVoteToast = Toast.makeText(context,
            context.getString(R.string.voting_up_message)+ " " + title, Toast.LENGTH_SHORT);
      upButton.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
          v.setEnabled(false);
          upVoteSong(libId);
          upVoteToast.show();
        }
      });

      final Toast downVoteToast = Toast.makeText(context,
            context.getString(R.string.voting_down_message)+ " " + title, Toast.LENGTH_SHORT);
      downButton.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
          v.setEnabled(false);
          downVoteSong(libId);
          downVoteToast.show();
        }
      });

      if(isCurrentlyPlaying){
        upButton.setEnabled(false);
        downButton.setEnabled(false);
      }
      else if(currentEntry.upvoters.contains(me)){
        upButton.setEnabled(false);
      }
      else if(currentEntry.downvoters.contains(me)){
        downButton.setEnabled(false);
      }
    }
    */

    private void upVoteSong(String libId) {
      voteOnSong(libId, 1);
    }

    private void downVoteSong(String libId) {
      voteOnSong(libId, -1);
    }

    private void voteOnSong(String libId, int voteType) {
      Intent voteIntent = new Intent(Intent.ACTION_INSERT,
          UDJPlayerProvider.VOTES_URI, context,
          PlaylistSyncService.class);
      voteIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
      voteIntent.putExtra(Constants.VOTE_WEIGHT_EXTRA, voteType);
      voteIntent.putExtra(Constants.LIB_ID_EXTRA, libId);
      context.startService(voteIntent);
    }


  }

}
