package com.stc.radio.player;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by artem on 9/23/16.
 */

public class SettingsManager {
	public static final String TAG = "PlaylistManager";

	public static final String SHARED_PREFS = "com.android.murano500k.SHARED_PREFS";
	private static final String SHARED_PREFS_SHUFFLE = "com.android.murano500k.SHARED_PREFS_SHUFFLE";
	private static final String SHARED_PREFS_URLS = "com.android.murano500k.SHARED_PREFS_URLS";
	private static final String SHARED_PREFS_SELECTED_URL = "com.android.murano500k.SHARED_PREFS_SELECTED_URL";
	private static final String SHARED_PREFS_PLAYLIST_INDEX = "com.android.murano500k.SHARED_PREFS_PLAYLIST_INDEX";
	private static SharedPreferences preferences;

	public SettingsManager(Context context) {
		preferences=context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
	}






	public String getSelectedUrl() {
		String urlSelected = preferences.getString(SHARED_PREFS_SELECTED_URL, null);
		return urlSelected;
	}

	void setSelectedUrl(String url) {
		preferences.edit().putString(SHARED_PREFS_SELECTED_URL, url).apply();
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

}
