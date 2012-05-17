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

import android.net.Uri;

public class Constants{
  /** Constants used for storing account info */
  public static final String ACCOUNT_TYPE = "org.klnusbaum.udj";
  public static final String AUTHORITY = "org.klnusbaum.udj";
  public static final String USER_ID_DATA = "org.klnusbaum.udj.userid";
  public static final String LAST_PLAYER_ID_DATA = "org.klnusbaum.udj.PlayerId";
  public static final String PLAYER_NAME_DATA = "org.klnusbaum.udj.PlayerName";
  public static final String PLAYER_HOSTNAME_DATA = 
    "org.klnusbaum.udj.PlayerHostname";
  public static final String PLAYER_LAT_DATA = "org.klnusbaum.udj.PlayerLat";
  public static final String PLAYER_LONG_DATA = "org.klnusbaum.udj.PlayerLong";
  public static final String PLAYER_JOIN_ERROR = "org.klnusbaum.udj.PlayerJoinError";


  public static final String PLAYER_HOST_ID_DATA = "org.klnusbaum.udj.PlayerOwnerId";
  public static final long NO_PLAYER_ID = -1;

  public static final String PLAYER_STATE_DATA = "org.klusbaum.udj.PlayerState";
  public static final int PLAYER_JOIN_FAILED = -1;
  public static final int NOT_IN_PLAYER = 0;
  public static final int JOINING_PLAYER = 1;
  public static final int IN_PLAYER = 2;
  public static final int LEAVING_PLAYER = 3;
  public static final int PLAYER_ENDED = 4;

  public static final String PLAYBACK_STATE_DATA = "org.klnusbaum.udj.PlaybackState";
  public static final int PLAYING_STATE = 0;
  public static final int PAUSED_STATE = 1;


  /** Constants use for passing account related info in intents */
  public static final String ACCOUNT_EXTRA = "org.klnusbaum.udj.account";
  public static final String PLAYER_ID_EXTRA = "org.klnusbaum.udj.PlayerId";
  public static final String VOTE_WEIGHT_EXTRA = "org.klnusbaum.udj.VoteType";
  public static final String PLAYLIST_ID_EXTRA = "org.klnusbaum.udj.PlaylistId";
  public static final String LIB_ID_EXTRA = "org.klnusbaum.udj.LibId";
  public static final String PLAYER_NAME_EXTRA = "org.klnusbaum.udj.PlayerName";
  public static final String PLAYER_OWNER_EXTRA = 
    "org.klnusbaum.udj.PlayerOwnerName";
  public static final String PLAYER_OWNER_ID_EXTRA = "org.klnusbaum.udj.PlaywerOwnderId";
  public static final String PLAYER_LAT_EXTRA = "org.klnusbaum.udj.PlayerLat";
  public static final String PLAYER_LONG_EXTRA = "org.klnusbaum.udj.PlayerLong";
  public static final String PLAYER_JOIN_ERROR_EXTRA = "org.klnusbaum.udj.PlayerJoinError";
  public static final String PLAYER_EXTRA = "org.klnusbaum.udj.Player";
  public static final String PLAYER_PASSWORD_EXTRA = "org.klnusbaum.udj.PlayerPassword";


  /** Constants for actions used throughout */
  public static final String ADD_REQUESTS_SYNCED = 
    "org.klnusbaum.udj.addRequestsSynced";
  public static final String LEFT_PLAYER_ACTION = "org.klnusbaum.udj.LeftPlayer";
  public static final String PLAYER_INACTIVE_ACTION = 
    "org.klnusbaum.udj.PlayerInactive";
  public static final String JOINED_PLAYER_ACTION = 
    "org.klnusbaum.udj.JoinedPlayer";
  public static final String PLAYER_JOIN_FAILED_ACTION = 
    "org.klnusbaum.udj.PlayerJoinFailed";
  public static final String SHOW_TOAST_ACTION = "org.klnusbaum.udj.ShowToast";

  /** URI constants */
  public static final Uri PLAYER_URI = new Uri.Builder().
    authority(Constants.AUTHORITY).appendPath("player").build();

  /** Error constants */
  public static final int AUTH_API_VERSION_ERROR = 1;

  public static final String ACTION_SET_CURRENT_SONG = "org.klnusbaum.udj.SetCurrentSong";

  public static final String ACTION_SET_PLAYBACK = "org.klnusbaum.udj.SetPlayback";
  public static final String BROADCAST_PLAYBACK_CHANGED = "org.klnusbaum.udj.PlaybackChanged";


  public static final String PLAYBACK_STATE_EXTRA = "org.klnusbaum.udj.PlaybackState";

}
