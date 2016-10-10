package com.stc.radio.player.utils;

/**
 * Created by artem on 9/9/16.
 */

public class UrlManager {
	//private final SharedPreferences preferences;
	public static final String TAG = "UrlManager";
	private static final String SHARED_PREFS_CUSTOM_PLAYLIST_STRING =
			"com.stc.radio.player.SHARED_PREFS_CUSTOM_PLAYLIST_STRING";
	private static final String PLAYLIST_BASE_URL = "https://api.friezy.ru/playlists/m3u/";
	public static final String RR = "RR";
	public static final String RT = "RT";
	public static final String CR = "CR";
	public static final String JR = "JR";
	public static final String DI = "DI";

	public static String getPlaylistUrl(String playlist_name) {
		return PLAYLIST_BASE_URL + playlist_name + ".php";
	}




/*
	public UrlManager(Context context) {
		this.preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
	}

	public void saveCustomPlaylistString(String s) {
		preferences.edit().putString(SHARED_PREFS_CUSTOM_PLAYLIST_STRING, s).apply();
	}

	public String getSavedCustomPlaylistString() {
		return preferences.getString(SHARED_PREFS_CUSTOM_PLAYLIST_STRING, null);
	}*/
}
