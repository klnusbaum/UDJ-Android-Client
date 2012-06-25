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
import android.app.Activity;
import android.accounts.Account;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.app.ListFragment;


import java.util.List;

public class ArtistsDisplayFragment extends ListFragment
  implements LoaderManager.LoaderCallbacks<ArtistsLoader.ArtistsResult>
{
  private static final int ARTISTS_LOADER_TAG = 0;

  private static final String TAG = "ArtistsDisplayFragment";

  private int previousVisiblePosition;
  private int previousVisibleIndex;
  private List<String> currentArtistsList;

  //private ArtistsAdapter artistsAdapter;
  private ArrayAdapter<String> artistsAdapter;

  private Account getAccount(){
    return (Account)getArguments().getParcelable(Constants.ACCOUNT_EXTRA);
  }

  @Override
  public void onAttach(Activity activity){
    super.onAttach(activity);
    Log.d(TAG, "Attaching artist view");
    artistsAdapter =
      new ArrayAdapter<String>(getActivity(), R.layout.artist_list_item, R.id.artist_name);
    getLoaderManager().initLoader(ARTISTS_LOADER_TAG, null, this);
    setListAdapter(artistsAdapter);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);

    setEmptyText(getActivity().getString(R.string.no_artists));

    setListShown(false);
    getListView().setTextFilterEnabled(true);
  }

  public Loader<ArtistsLoader.ArtistsResult> onCreateLoader(
    int id, Bundle args)
  {
    Log.d(TAG, "In creation of loader");
    if(id == ARTISTS_LOADER_TAG){
      return new ArtistsLoader(getActivity(), getAccount());
    }
    return null;
  }

  public void onLoadFinished(
    Loader<ArtistsLoader.ArtistsResult> loader,
    ArtistsLoader.ArtistsResult data)
  {
    Log.d(TAG, "In loader finshed");
    if(data.error == ArtistsLoader.ArtistsError.NO_ERROR){
      if(currentArtistsList == null || !currentArtistsList.equals(data.res)){
        Log.d(TAG, "Changing artist list");
        currentArtistsList = data.res;
        artistsAdapter.clear();
        artistsAdapter.addAll(data.res);
      }
    }
    else if(data.error == ArtistsLoader.ArtistsError.PLAYER_INACTIVE_ERROR){
      Utils.handleInactivePlayer(getActivity(), getAccount());
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
    Log.d(TAG, "Loader Was reset");
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

