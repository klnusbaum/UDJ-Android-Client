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

import android.support.v4.content.Loader;

import android.os.Bundle;
import android.accounts.Account;
import android.app.SearchManager;
import android.provider.SearchRecentSuggestions;

public class RegularSearchFragment extends SearchFragment{

  public void onCreate(Bundle icicle){
    super.onCreate(icicle);
    String query = getActivity().getIntent().getStringExtra(SearchManager.QUERY);
    SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
      MusicSearchSuggestionProvider.AUTHORITY, MusicSearchSuggestionProvider.MODE);
    suggestions.saveRecentQuery(query, null);

  }

  public Loader<MusicSearchLoader.MusicSearchResult> getLoader(Account account){
    String searchQuery = getActivity().getIntent().getStringExtra(SearchManager.QUERY);
    return new RegularSearchLoader(getActivity(), searchQuery, account);
  }

  public boolean linksArtistNames(){
    return true;
  }
}
