package com.stc.radio.player.contentmodel;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by artem on 10/12/16.
 */

public interface AuthInterface {
	@GET("/")
	Call<AuthData> getAuthData();
}



