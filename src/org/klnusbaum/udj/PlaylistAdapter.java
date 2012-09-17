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

import org.klnusbaum.udj.containers.ActivePlaylistEntry;
import org.klnusbaum.udj.containers.User;
import org.klnusbaum.udj.network.PlaylistSyncService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.Intent;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public class PlaylistAdapter extends BaseAdapter{
  private static final String PLAYLIST_ADAPTER_TAG = "PlaylistAdapter";
  private static final int CURRENT_SONG_VIEW_TYPE = 0;
  private static final int REGULAR_SONG_VIEW_TYPE = 1;
  private List<ActivePlaylistEntry> playlist;
  private String userId;
  private Context context;
  private final PlaylistFragment plFrag;
  private Account account;
  private User me;
  private ConcurrentMap<String, Long> idMap;
  private long currentAvailableMapId;

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
    this.currentAvailableMapId = 0;
    idMap = new ConcurrentHashMap<String, Long>();
    if(playlist != null){
      for(ActivePlaylistEntry ae: playlist){
        idMap.put(ae.song.getLibId(), currentAvailableMapId);
        currentAvailableMapId++;
      }
    }
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
    return idMap.get(playlist.get(position).song.getLibId());
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

  public synchronized void updatePlaylist(List<ActivePlaylistEntry> newPlaylist){
    for(ActivePlaylistEntry ae: newPlaylist){
      if (!idMap.keySet().contains(ae.song.getLibId())){
        idMap.put(ae.song.getLibId(), currentAvailableMapId);
        currentAvailableMapId++;
      }
    }
    this.playlist = newPlaylist;
    /**
     * This algorithm is n*m complexity, might be able to do better...
     */
    for(String s: idMap.keySet()){
      if(!playlistContainsId(s)){
        idMap.remove(s);
      }
    }
    notifyDataSetChanged();
  }

  private synchronized boolean playlistContainsId(String s){
    for(ActivePlaylistEntry ae: playlist){
      if(s.equals(ae.song.getLibId())){
        return true;
      }
    }
    return false;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ActivePlaylistEntry currentEntry = (ActivePlaylistEntry)getItem(position);
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
    upCount.setText(String.valueOf(currentEntry.upvoters.size()));
    downCount.setText(String.valueOf(currentEntry.downvoters.size()));

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
    final ActivePlaylistEntry currentEntry, View convertView, ViewGroup parent)
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

    upButton.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
        currentEntry.downvoters.remove(me);
        currentEntry.upvoters.add(me);
        upVoteSong(libId);
        notifyDataSetChanged();
      }
    });

    downButton.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
        currentEntry.downvoters.add(me);
        currentEntry.upvoters.remove(me);
        downVoteSong(libId);
        notifyDataSetChanged();
      }
    });

    /* Reset buttons from previous view*/
    upButton.setEnabled(true);
    downButton.setEnabled(true);
    if(currentEntry.upvoters.contains(me)){
      upButton.setEnabled(false);
    }
    else if(currentEntry.downvoters.contains(me)){
      downButton.setEnabled(false);
    }

    return view;
  }

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


