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
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.util.Log;

import java.util.List;

import org.klnusbaum.udj.containers.Player;

public class PlayerListAdapter implements ListAdapter{

  public static final String TAG = "PlayerListAdapter";
  private List<Player> players;
  private Context context;
  public static final int PLAYER_ENTRY_VIEW_TYPE = 0;

  public PlayerListAdapter(Context context){
    this.players = null;
    this.context = context;
  }

  public PlayerListAdapter(
    Context context, 
    List<Player> players
  )
  {
    this.players = players;
    this.context = context;
  }

  public boolean areAllItemsEnabled(){
    return true;
  }

  public boolean isEnabled(int position){
    return true;
  }

  public int getCount(){
    if(players != null){
      return players.size();
    }
    return 0;
  }

  public Object getItem(int position){
    if(players != null){
      return players.get(position);
    }
    return null;
  }

  public Player getPlayer(int position){
    if(players != null){
      return players.get(position);
    }
    return null;
  }

  public long getItemId(int position){
    if(players != null){
      return players.get(position).getPlayerId();
    }
    return 0; 
  }

  public int getItemViewType(int position){
    return PLAYER_ENTRY_VIEW_TYPE;
  }

  public View getView(int position, View convertView, ViewGroup parent){
    //TODO should probably enforce view type
    Player player = getPlayer(position);
    View toReturn = convertView;
    if(toReturn == null){
      //toReturn = View.inflate(context, R.layout.library_list_item, null);
      LayoutInflater inflater = (LayoutInflater)context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
      toReturn = inflater.inflate(R.layout.player_list_item, null);
    }

    TextView playerName = (TextView)toReturn.findViewById(R.id.player_item_name);
    TextView hostName = 
      (TextView)toReturn.findViewById(R.id.player_host_name);
    playerName.setText(player.getName());
    hostName.setText(player.getOwnerName());
    if(player.getHasPassword()){
      Log.d(TAG, "Unhidding lock");
      ImageView lock = (ImageView)toReturn.findViewById(R.id.lock_icon);
      lock.setVisibility(0);
    }
    return toReturn;
  }

  public int getViewTypeCount(){
    return 1; 
  }

  public boolean hasStableIds(){
    return true;
  }

  public boolean isEmpty(){
    if(players != null){
      return players.isEmpty();
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

}
