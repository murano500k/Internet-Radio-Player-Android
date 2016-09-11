package com.android.murano500k.newradio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.murano500k.newradio.rest.RadioApiService;
import com.android.murano500k.newradio.rest.ResponceRegister;
import com.android.murano500k.newradio.ui.ActivityRxTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.android.murano500k.newradio.PlaylistManager.SHARED_PREFS;

/**
 * Created by artem on 9/9/16.
 */

public class UrlManager {
	private final Context context;
	private final SharedPreferences preferences;
	public static final String TAG="UrlManager";
	public static final String SHARED_PREFS_TOKEN_EXPIRES_AT =
			"com.android.murano500k.newradio.SHARED_PREFS_TOKEN_EXPIRED";
	public static final String SHARED_PREFS_TOKEN =
			"com.android.murano500k.newradio.SHARED_PREFS_TOKEN";
	public static final String SHARED_PREFS_CUSTOM_URL =
			"com.android.murano500k.newradio.SHARED_PREFS_CUSTOM_URL";
	public static final String SHARED_PREFS_CUSTOM_PLAYLIST_STRING =
			"com.android.murano500k.newradio.SHARED_PREFS_CUSTOM_PLAYLIST_STRING";
	public static final String PLAYLIST_BASE_URL="https://api.friezy.ru/playlists/m3u/";
	public static final String RR="RR";
	public static final String RT="RT";
	public static final String CR="CR";
	public static final String JR="JR";
	public static final String DI="DI";
	public static String getPlaylistUrl(String playlist_name){
			return PLAYLIST_BASE_URL+playlist_name+".php";
	}
	public String getPlaylistUrl(int playlist_index){
		switch (playlist_index){
			case INDEX_DI:
				return getPlaylistUrl(DI);
			case INDEX_RR:
				return getPlaylistUrl(RR);
			case INDEX_CR:
				return getPlaylistUrl(CR);
			case INDEX_RT:
				return getPlaylistUrl(RT);
			case INDEX_JR:
				return getPlaylistUrl(JR);
			case INDEX_CUSTOM:
				return "undefined";
			default:
				return getPlaylistUrl(DI);
		}
	}

	public static final int INDEX_DI=1;
	public static final int INDEX_RT=2;
	public static final int INDEX_JR =3;
	public static final int INDEX_RR =4;
	public static final int INDEX_CR=5;
	public static final int INDEX_CUSTOM=6;


	private static final String baseApiUrl = "https://api.friezy.ru/";
	private static final String plsPath = "playlists/m3u/";

	String token;
	long token_expires_at;

	public UrlManager(Context context) {
		this.context=context;
		this.preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
		token=null;
		token_expires_at=-1;
	}


	public long getTokenExpiresFromPrefs(){
		return preferences.getLong(SHARED_PREFS_TOKEN_EXPIRES_AT, -1);
	}
	public String getTokenFromPrefs(){
		return preferences.getString(SHARED_PREFS_TOKEN, null);
	}

	public boolean hasValidToken(){
		if(token==null || token_expires_at<0) updateToken();
		return Calendar.getInstance().getTime().getTime()<token_expires_at;
	}

	public void saveNewToken(ResponceRegister responceRegister){
		token_expires_at=Long.parseLong(responceRegister.getTimeExp());
		token= responceRegister.getToken();
		preferences.edit().putLong(SHARED_PREFS_TOKEN_EXPIRES_AT,token_expires_at).apply();
		preferences.edit().putString(SHARED_PREFS_TOKEN, token).apply();
	}
	public boolean updateToken(){
		if(token==null) token=getTokenFromPrefs();
		if(token_expires_at<0) token_expires_at=getTokenExpiresFromPrefs();
		if(token==null || token_expires_at<0) {
			rx.Observable.just(1)
					.observeOn(Schedulers.newThread())
					.subscribeOn(AndroidSchedulers.mainThread())
					.subscribe(s -> {
						saveNewToken(getResponceRegister());
						Intent intent = new Intent(context, ActivityRxTest.class);
						intent.setAction(ActivityRxTest.INTENT_TOKEN_UPDATED);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
					});
			return false;
		}
		return true;
		//TODO check stations count
	}
	public String generateUrl(String radioName,String stationName){
		if(token==null
				|| token_expires_at<0
				|| !hasValidToken()
				|| !updateToken())
			return null;
		return "http://prem"+(new Random().nextInt(3)+1)+"."+radioName+".com:80/"+stationName+"_hi?"+ token;
	}


	public ResponceRegister getResponceRegister() {
		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
				.create();

		Retrofit retrofit = new Retrofit.Builder()
				.addConverterFactory(GsonConverterFactory.create(gson))
				.baseUrl("https://api.friezy.ru/")
				.build();

		RadioApiService apiService = retrofit.create(RadioApiService.class);
		try {
			retrofit2.Response<ResponceRegister> response=apiService.get_token().execute();
			if(response.isSuccessful()) Log.d(TAG, "SUCCESS");
			else {
				Log.d(TAG, "FAIL: "+ response.errorBody().toString());
				return null;
			}

			return response.body();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void saveCustomPlaylistString(String s) {
		preferences.edit().putString(SHARED_PREFS_CUSTOM_PLAYLIST_STRING, s).apply();
	}

	public String getSavedCustomPlaylistString() {
		return preferences.getString(SHARED_PREFS_CUSTOM_PLAYLIST_STRING, null);
	}

	public static final class RadioList{
		private final String name;
		private final String[] stationNames;
		public RadioList(String name, String[] stationNames) {
			this.name = name;
			this.stationNames = stationNames;
		}

		public String getName() {
			return name;
		}

		public String[] getStationNames() {
			return stationNames;
		}
	}
}
