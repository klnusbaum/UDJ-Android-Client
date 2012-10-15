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
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public class PlaylistAdapter extends StringIdableAdapter{
  private static final String PLAYLIST_ADAPTER_TAG = "PlaylistAdapter";
  private static final int CURRENT_SONG_VIEW_TYPE = 0;
  private static final int REGULAR_SONG_VIEW_TYPE = 1;
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
    super(playlist);
    this.userId = userId;
    this.context = context;
    this.plFrag = plFrag;
    this.account = account;
    this.me = new User(userId);
  }

  public ActivePlaylistEntry getPlaylistEntry(int position){
    if(!isEmpty()){
      return (ActivePlaylistEntry)getItem(position);
    }
    return null;
  }

  public synchronized void setNewCurrentSong(ActivePlaylistEntry toSet){
    if(getPlaylistEntry(0).isCurrentSong()){
      removeItem(0);
    }
    removeItem(toSet);
    toSet.setCurrentSong(true);
    addItem(0,toSet);
  }

  public int getViewTypeCount(){
    return 2;
  }

  public int getItemViewType(int position){
    if(!isEmpty() && ((ActivePlaylistEntry)getItem(position)).isCurrentSong()){
      return CURRENT_SONG_VIEW_TYPE;
    }
    return REGULAR_SONG_VIEW_TYPE;
  }



  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final ActivePlaylistEntry currentEntry = getPlaylistEntry(position);
    final String libId = currentEntry.getSong().getId();
    View view;
    switch(getItemViewType(position)){
      case CURRENT_SONG_VIEW_TYPE:
        view = getCurrentSongView(convertView, parent);
        break;
      default:
        view = getRegularSongView(currentEntry, convertView, parent);
    }

    final TextView songName = (TextView) view.findViewById(R.id.playlistSongName);
    final String title = currentEntry.getSong().getTitle();
    songName.setText(title);

    final TextView artist = (TextView) view.findViewById(R.id.playlistArtistName);
    artist.setText(context.getString(R.string.by) + " " + currentEntry.getSong().getArtist());

    final TextView addByUser = (TextView) view.findViewById(R.id.playlistAddedBy);
    if (currentEntry.getAdder().equals(me)) {
      addByUser.setText(context.getString(R.string.added_by) 
          + " " + context.getString(R.string.you));
    }
    else{
      addByUser.setText(context.getString(R.string.added_by) + " " + currentEntry.getAdder().getUsername());
    }

    final TextView upCount = (TextView) view.findViewById(R.id.upcount);
    final TextView downCount = (TextView) view.findViewById(R.id.downcount);
    upCount.setText(String.valueOf(currentEntry.getUpvoters().size()));
    downCount.setText(String.valueOf(currentEntry.getDownvoters().size()));

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
    final String libId = currentEntry.getSong().getId();
    final String title = currentEntry.getSong().getTitle();
    final ImageButton upButton = (ImageButton)view.findViewById(R.id.upvote_button);
    final ImageButton downButton = (ImageButton)view.findViewById(R.id.downvote_button);

    upButton.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
        currentEntry.removeFromDownvoters(me);
        currentEntry.addToUpvoters(me);
        upVoteSong(libId);
        notifyDataSetChanged();
      }
    });

    downButton.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
        currentEntry.addToDownvoters(me);
        currentEntry.removeFromUpvoters(me);
        downVoteSong(libId);
        notifyDataSetChanged();
      }
    });

    /* Reset buttons from previous view*/
    upButton.setEnabled(true);
    downButton.setEnabled(true);
    if(currentEntry.getUpvoters().contains(me)){
      upButton.setEnabled(false);
    }
    else if(currentEntry.getDownvoters().contains(me)){
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
        Constants.VOTES_URI, context,
        PlaylistSyncService.class);
    voteIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
    voteIntent.putExtra(Constants.VOTE_WEIGHT_EXTRA, voteType);
    voteIntent.putExtra(Constants.LIB_ID_EXTRA, libId);
    context.startService(voteIntent);
  }
}


