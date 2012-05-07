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

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SearchViewCompat;

import android.os.Bundle;
import android.content.Intent;
import android.app.SearchManager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.actionbarsherlock.app.SherlockFragmentActivity;


/**
 * Class used for displaying the contents of the Playlist.
 */
public class PlayerSelectorActivity extends SherlockFragmentActivity{


  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    FragmentManager fm = getSupportFragmentManager();
    if(fm.findFragmentById(android.R.id.content) == null){
      PlayerListFragment list = new PlayerListFragment();
      fm.beginTransaction().add(android.R.id.content, list).commit();
    }
  }

  private PlayerListFragment getPlayerList(){
    return
      ((PlayerListFragment)getSupportFragmentManager().findFragmentById(android.R.id.content));
  }

  public boolean onCreateOptionsMenu(Menu menu){
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
    return false;
  }


  protected void onNewIntent(Intent intent){
    if(Intent.ACTION_SEARCH.equals(intent.getAction())){
      PlayerListFragment list = getPlayerList();
      list.setPlayerSearch(new PlayerListFragment.NamePlayerSearch(
        intent.getStringExtra(SearchManager.QUERY)));
    }
    else{
      super.onNewIntent(intent);
    }
  }


  public void refreshList(){
    getPlayerList().refreshList();
  }

}
