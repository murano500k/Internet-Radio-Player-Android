package com.android.murano500k.newradio.rest;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by artem on 9/9/16.
 */

public interface RadioApiService {
	@GET("https://api.friezy.ru/")
	Call<ResponceRegister> get_token();
}
