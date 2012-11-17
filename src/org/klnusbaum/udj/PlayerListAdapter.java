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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.klnusbaum.udj.containers.Player;

public class PlayerListAdapter extends StringIdableAdapter<Player>{

  public static final String TAG = "PlayerListAdapter";
  private Context context;
  public static final int PLAYER_ENTRY_VIEW_TYPE = 0;

  public PlayerListAdapter(Context context){
    super(null);
    this.context = context;
  }

  public PlayerListAdapter(
    Context context, 
    List<Player> players
  )
  {
    super(players);
    this.context = context;
  }

  public Player getPlayer(int position){
    if(!isEmpty()){
      return (Player)getItem(position);
    }
    return null;
  }

  public int getItemViewType(int position){
    return PLAYER_ENTRY_VIEW_TYPE;
  }

  public View getView(int position, View convertView, ViewGroup parent){
    //TODO should probably enforce view type
    Player player = getPlayer(position);
    View toReturn = convertView;
    if(toReturn == null){
      LayoutInflater inflater = (LayoutInflater)context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
      toReturn = inflater.inflate(R.layout.player_list_item, null);
    }

    TextView playerName = (TextView)toReturn.findViewById(R.id.player_item_name);
    TextView hostName = 
      (TextView)toReturn.findViewById(R.id.player_host_name);
    playerName.setText(player.getName());
    hostName.setText(player.getOwnerName());
    ImageView lock = (ImageView)toReturn.findViewById(R.id.lock_icon);
    if(player.getHasPassword()){
      lock.setVisibility(View.VISIBLE);
    }
    else{
      lock.setVisibility(View.INVISIBLE);
    }
    return toReturn;
  }

  public int getViewTypeCount(){
    return 1; 
  }

}
