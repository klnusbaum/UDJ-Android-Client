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

import android.os.Bundle;
import android.accounts.AccountManager;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;


import org.klnusbaum.udj.Constants;
import org.klnusbaum.udj.network.PlaylistSyncService;

/**
 * The main activity display class.
 */
public class PlayerActivity extends PlayerInactivityListenerActivity {
	private static final String TAG = "EventActivity";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		// TODO hanle if no event
		getPlaylistFromServer();
	}

	public void getPlaylistFromServer() {
		int eventState = Utils.getPlayerState(this, account);
		if (eventState == Constants.IN_PLAYER) {
			Intent getPlaylist = new Intent(Intent.ACTION_VIEW,
					UDJPlayerProvider.PLAYLIST_URI, this,
					PlaylistSyncService.class);
			getPlaylist.putExtra(Constants.ACCOUNT_EXTRA, account);
			startService(getPlaylist);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.player, menu);
		return true;
	}

	private PlaylistFragment getPlaylist() {
		return ((PlaylistFragment) getSupportFragmentManager()
				.findFragmentById(R.id.playlist));
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			PlaylistFragment list = getPlaylist();
			list.setListShown(false);
			getPlaylistFromServer();
			return true;
		case R.id.menu_search:
			startSearch(null, false, null, false);
			return true;
		case R.id.menu_random:
			doRandomSearch();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "In on new intent");
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			intent.setClass(this, MusicSearchActivity.class);
			startActivityForResult(intent, 0);
		}
	}

	private void doRandomSearch() {
		Intent randomIntent = new Intent(this, RandomSearchActivity.class);
		startActivity(randomIntent);
	}
	
	public void onBackPressed(){
	    AccountManager am = AccountManager.get(this);
	    Utils.leavePlayer(am, account);
	    finish();
	}
}
