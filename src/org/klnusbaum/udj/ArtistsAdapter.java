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

import android.widget.ListAdapter;
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
/*
import android.widget.Filterable;
import android.widget.Filter;
*/

import java.util.List;


public class ArtistsAdapter implements ListAdapter{
  private static final int ARTIST_ENTRY_VIEW_TYPE = 0;
  private List<String> artists;
  private Context context;
  private Account account;

  public ArtistsAdapter(Context context, Account account){
    this.artists = null;
    this.context = context;
    this.account = account;
  }

  public ArtistsAdapter(Context context, List<String> artists, Account account){
    this.artists = artists;
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
    if(artists != null){
      return artists.size();
    }
    return 0;
  }

  public Object getItem(int position){
    if(artists != null){
      return artists.get(position);
    }
    return null;
  }

  public String getArtist(int position){
    if(artists != null){
      return artists.get(position);
    }
    return null;
  }

  public long getItemId(int position){
    if(artists != null){
      //return artists.get(position).getLibId();
      return position;
    }
    return -1; 
  }

  public int getItemViewType(int position){
    return ARTIST_ENTRY_VIEW_TYPE;
  }

  public View getView(int position, View convertView, ViewGroup parent){
    //TODO should probably enforce view type
    final String artist = getArtist(position);
    View toReturn = convertView;
    if(toReturn == null){
      LayoutInflater inflater = (LayoutInflater)context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
      toReturn = inflater.inflate(R.layout.artist_list_item, null);
    }

    TextView artistNameView = (TextView)toReturn.findViewById(R.id.artist_name);
    artistNameView.setText(artist);
    return toReturn;

  }

  public int getViewTypeCount(){
    return 1; 
  }

  public boolean hasStableIds(){
    return true;
  }

  public boolean isEmpty(){
    if(artists != null){
      return artists.isEmpty();
    }
    return true;
  }

  public void registerDataSetObserver(DataSetObserver observer){
    //Unimplemented because this data can't change
    //If new results need to be displayed a new adpater should be created.
  }

  public void unregisterDataSetObserver(DataSetObserver observer){
    //Unimplemented because data represented by this adpater shouldn't change.
    //If new results need to be displayed a new adpater should be created.
  }

  /*
  public getFilter(){
    return new ArtistsFilter();
  }

  public static class ArtistsFilter extends Filter{
    protected Filter.FilterResults performFiltering(CharSequence constraint){

    }

    protected void publishResults(CharSequence constraint, Filter.FilterResults results){

    }
  }*/


}
