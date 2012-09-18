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

import android.widget.BaseAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.TextView;
import android.widget.ImageButton;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.content.Intent;
import android.accounts.Account;
import android.app.SearchManager;
import android.graphics.Color;
import android.text.Html;

import java.util.List;

import org.klnusbaum.udj.containers.LibraryEntry;
import org.klnusbaum.udj.network.PlaylistSyncService;

public class MusicSearchAdapter extends BaseAdapter{

  private List<LibraryEntry> entries;
  private Context context;
  private Account account;
  public static final int LIB_ENTRY_VIEW_TYPE = 0;

  public MusicSearchAdapter(Context context, Account account){
    this.entries = null;
    this.context = context;
    this.account = account;
  }

  public MusicSearchAdapter(
    Context context,
    List<LibraryEntry> entries,
    Account account
  )
  {
    this.entries = entries;
    this.context = context;
    this.account = account;
  }

  public boolean areAllItemsEnabled(){
    return true;
  }

  public boolean isEnabled(int position){
    return true;
  }

  public int getCount(){
    if(entries != null){
      return entries.size();
    }
    return 0;
  }

  @Override
  public boolean hasStableIds(){
    return false;
  }

  public void setData(List<LibraryEntry> newData){
    if(!newData.equals(entries)){
      entries = newData;
      notifyDataSetChanged();
    }
  }

  public Object getItem(int position){
    if(entries != null){
      return entries.get(position);
    }
    return null;
  }

  public LibraryEntry getLibraryEntry(int position){
    if(entries != null){
      return entries.get(position);
    }
    return null;
  }

  public long getItemId(int position){
    if(entries != null){
      return position;
    }
    return -1; 
  }

  public int getItemViewType(int position){
    return LIB_ENTRY_VIEW_TYPE;
  }

  public View getView(int position, View convertView, ViewGroup parent){
    //TODO should probably enforce view type
    final LibraryEntry libEntry = getLibraryEntry(position);
    View toReturn = convertView;
    if(toReturn == null){
      LayoutInflater inflater = (LayoutInflater)context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
      toReturn = inflater.inflate(R.layout.library_list_item, null);
    }


    TextView songView = (TextView)toReturn.findViewById(R.id.librarySongName);
    TextView artistView =
      (TextView)toReturn.findViewById(R.id.libraryArtistName);
    ImageButton addButton =
      (ImageButton)toReturn.findViewById(R.id.lib_add_button);
    songView.setText(libEntry.getTitle());
    final String artistContent = context.getString(R.string.by) + " " + libEntry.getArtist();
    artistView.setText(Html.fromHtml("<u>" + artistContent + "</u>"));
    artistView.setOnClickListener(new View.OnClickListener(){
      public void onClick(View v){
        final String artist = libEntry.getArtist();
        Intent artistIntent = new Intent(Intent.ACTION_SEARCH);
        artistIntent.setClass(context, ArtistSearchActivity.class);
        artistIntent.putExtra(SearchManager.QUERY, artist);
        context.startActivity(artistIntent);
      }
    });

    //Reset addbutton
    addButton.setEnabled(true);
    if(libEntry.getIsAdded()){
      addButton.setEnabled(false);
    }

    addButton.setOnClickListener(
      new View.OnClickListener(){
        public void onClick(View v){
          Intent addSongIntent = new Intent(
            Intent.ACTION_INSERT,
            UDJPlayerProvider.PLAYLIST_URI,
            context,
            PlaylistSyncService.class);
          addSongIntent.putExtra(Constants.ACCOUNT_EXTRA, account);
          addSongIntent.putExtra(Constants.LIB_ID_EXTRA, libEntry.getLibId());
          context.startService(addSongIntent);
          libEntry.setIsAdded(true);
          notifyDataSetChanged();
        }
      });
    return toReturn;
  }

  public int getViewTypeCount(){
    return 1;
  }

  public boolean isEmpty(){
    if(entries != null){
      return entries.isEmpty();
    }
    return true;
  }
}
