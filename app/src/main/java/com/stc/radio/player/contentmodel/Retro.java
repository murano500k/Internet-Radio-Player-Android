package com.stc.radio.player.contentmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stc.radio.player.utils.SettingsProvider;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static com.stc.radio.player.utils.SettingsProvider.AUTH_EXPIRE_DATE;
import static com.stc.radio.player.utils.SettingsProvider.AUTH_TOKEN;
import static com.stc.radio.player.utils.SettingsProvider.prefs;

/**
 * Created by artem on 10/12/16.
 */

public class Retro {

	public static StationsInterface getStationsInterface() {

		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
				.create();

		// Add logging into retrofit 2.0
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://api.audioaddict.com")
				.addConverterFactory(GsonConverterFactory.create(gson))
				.client(httpClient.build()).build();
		return retrofit.create(StationsInterface.class);
	}
	public static retrofit2.Call<List<ParsedPlaylistItem>> getStationsCall(String pls) {
		Timber.w("get stations for name: %s", pls);
		return getStationsInterface().getPlaylistContent(pls);
	}

	public static retrofit2.Call<AuthData> getAuthDataCall() {

		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
				.create();

		// Add logging into retrofit 2.0
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://api.friezy.ru")
				.addConverterFactory(GsonConverterFactory.create(gson))
				.client(httpClient.build()).build();

		return retrofit.create(AuthInterface.class).getAuthData();
	}
	public void getAuthData(){

	}
	public static boolean hasValidToken(){
		if(prefs.contains(SettingsProvider.AUTH_TOKEN) && prefs.contains(AUTH_EXPIRE_DATE) ){
			long now = Calendar.getInstance().getTime().getTime();
			long willExpire= Long.parseLong(prefs.getString(AUTH_EXPIRE_DATE, "1000000"))-(1000*60*60*24);
			if(now<willExpire) {
				return true;
			}
		}
		return false;
	}
	public static String updateToken() {
		String token=null;
		try {
			Response<AuthData> response = getAuthDataCall().execute();
			if (response.isSuccessful()) {

				Timber.w("timexp: %s",response.body().getTimeExp());
				Timber.w("token: %s",response.body().getToken());
				token=response.body().getToken();
				prefs.edit().putString(AUTH_TOKEN, token).apply();
				prefs.edit().putString(AUTH_EXPIRE_DATE, response.body().getTimeExp()).apply();
				return token;
			}
		} catch (IOException e) {
			Timber.e(e);
		}
		return null;
	}
}



