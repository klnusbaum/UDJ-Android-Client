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

import android.accounts.Account;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;
import android.app.SearchManager;
import android.widget.ListView;
import android.util.Log;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.app.ListFragment;

import android.accounts.Account;

public class ArtistsDisplayFragment extends ListFragment
  implements LoaderManager.LoaderCallbacks<ArtistsLoader.ArtistsResult>
{
  private static final int ARTISTS_LOADER_TAG = 0;

  private static final String TAG = "ArtistDisplayFragment";

  //private ArtistsAdapter artistsAdapter;
  private ArrayAdapter<String> artistsAdapter;
  private Account account;

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);

    account = Utils.basicGetUdjAccount(getActivity());
    setEmptyText(getActivity().getString(R.string.no_artists));

    artistsAdapter =
      new ArrayAdapter<String>(getActivity(), R.layout.artist_list_item, R.id.artist_name);
    setListAdapter(artistsAdapter);
    setListShown(false);
    getListView().setTextFilterEnabled(true);
    getLoaderManager().initLoader(ARTISTS_LOADER_TAG, null, this);
  }

  public Loader<ArtistsLoader.ArtistsResult> onCreateLoader(
    int id, Bundle args)
  {
    if(id == ARTISTS_LOADER_TAG){
      return new ArtistsLoader(getActivity(), account);
    }
    return null;
  }

  public void onLoadFinished(
    Loader<ArtistsLoader.ArtistsResult> loader,
    ArtistsLoader.ArtistsResult data)
  {
    if(data.error == ArtistsLoader.ArtistsError.NO_ERROR){
      artistsAdapter = new ArrayAdapter<String>(
        getActivity(),
        R.layout.artist_list_item,
        R.id.artist_name,
        data.res
        );
      setListAdapter(artistsAdapter);
    }
    else if(data.error == ArtistsLoader.ArtistsError.PLAYER_INACTIVE_ERROR){
      Utils.handleInactivePlayer(getActivity(), account);
    }
    else if(data.error == ArtistsLoader.ArtistsError.PLAYER_AUTH_ERROR){
      //TODO REAUTH AND TRY AGAIN
    }

    if(isResumed()){
      setListShown(true);
    }
    else if(isVisible()){
      setListShownNoAnimation(true);
    }
  }

  public void onLoaderReset(Loader<ArtistsLoader.ArtistsResult> loader){
    setListAdapter(null);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id){
    Log.d(TAG, "In on list item clicked");
    final String artist = (String)artistsAdapter.getItem(position);
    Intent artistIntent = new Intent(Intent.ACTION_SEARCH);
    artistIntent.setClass(getActivity(), ArtistSearchActivity.class);
    artistIntent.putExtra(SearchManager.QUERY, artist);
    startActivityForResult(artistIntent, 0);
  }

}

