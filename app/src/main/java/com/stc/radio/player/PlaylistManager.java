package com.stc.radio.player;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;



public class PlaylistManager {
	public static final String TAG = "PlaylistManager";

	public static final String SHARED_PREFS = "com.android.murano500k.SHARED_PREFS";
	private static final String SHARED_PREFS_SHUFFLE = "com.android.murano500k.SHARED_PREFS_SHUFFLE";
	private static final String SHARED_PREFS_URLS = "com.android.murano500k.SHARED_PREFS_URLS";
	private static final String SHARED_PREFS_SELECTED_URL = "com.android.murano500k.SHARED_PREFS_SELECTED_URL";
	private static final String SHARED_PREFS_PLAYLIST_INDEX = "com.android.murano500k.SHARED_PREFS_PLAYLIST_INDEX";
	private SharedPreferences preferences;
	private static ArrayList<String> stationUrls=null;

	UrlManager getUrlManager() {
		return urlManager;
	}

	private UrlManager urlManager;


	private Context context;


	public PlaylistManager(Context context) {
		this.context=context;
		this.preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
		urlManager=new UrlManager();
	}



	public int getBufferSize() {
		return preferences.getInt(ServiceRadioRx.EXTRA_AUDIO_BUFFER_CAPACITY, 800);
	}

	void saveBufferSize(int size) {
		preferences.edit().putInt(ServiceRadioRx.EXTRA_AUDIO_BUFFER_CAPACITY, size).apply();
	}

	public int getDecodeSize() {
		return preferences.getInt(ServiceRadioRx.EXTRA_AUDIO_DECODE_CAPACITY, 800);
	}

	void saveDecodeSize(int size) {
		preferences.edit().putInt(ServiceRadioRx.EXTRA_AUDIO_DECODE_CAPACITY, size).apply();
	}


	public String getNameFromUrl(String requestUrl) {
		return preferences.getString(requestUrl, "station");
	}

	static String getArtistFromString(String data) {
		String artistName = "";
		if (data != null && data.contains(" - ")) {
			artistName = data.substring(0, data.indexOf(" - "));
		}
		return artistName;
	}

	static String getTrackFromString(String data) {
		String trackName = "";
		if (data != null && data.contains(" - ")) {
			trackName = data.substring(data.indexOf(" - ") + 3);
		}
		return trackName;
	}

	public int getArt(String url, Context c) {
		int resID = c.getResources().getIdentifier(getNameFromUrl(url), "drawable", c.getPackageName());
		if (resID != 0) return resID;
		else return R.drawable.ic_checked;
	}

	public void setStationFavorite(String url, boolean isFav) {//TODO FAVORITES
		/*Log.d(TAG, "setStationFavorite " + url + " isFav " + isFav);
		if (stationUrls == null || stationUrls.size() < 1) getStations();
		Set<String> stationsFav=loadUrls(SHARED_PREFS_URLS_FAV);
		if (isFav && !stationsFav.contains(url)) stationsFav.add(url);
		else if(!isFav && stationsFav.contains(url))  stationsFav.remove(url);
		preferences.edit().putStringSet(SHARED_PREFS_URLS_FAV, stationsFav).apply();
		stationsFav=null;
		getStations();*/
	}
	public boolean isOnlyFavorites() {//TODO FAVORITES
		//Log.d(TAG, "isOnlyFavorites " + res);
		//return preferences.getBoolean(SHARED_PREFS_PLS_TYPE, false);
		return false;
	}

	public void setOnlyFavorites(boolean onlyFav) {
		/*Log.d(TAG, "setOnlyFavorites " + onlyFav + " saved");
		preferences.edit().putBoolean(SHARED_PREFS_PLS_TYPE, onlyFav).apply();
		preferences.edit().apply();
		stationUrls =null;
		getStations();*/
	}


}





