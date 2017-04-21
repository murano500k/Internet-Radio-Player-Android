package com.stc.radio.player.model;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by artem on 10/12/16.
 */

public interface StationsInterface {
	@GET("/v1/{playlist}/channels.json")
	Call<List<ParsedPlaylistItem>>getPlaylistContent(@Path("playlist") String playlist);
}
