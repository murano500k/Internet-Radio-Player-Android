package com.android.murano500k.newradio;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

import static com.android.murano500k.newradio.ServiceRadioRx.EXTRA_AUDIO_BUFFER_CAPACITY;
import static com.android.murano500k.newradio.ServiceRadioRx.EXTRA_AUDIO_DECODE_CAPACITY;
import static com.android.murano500k.newradio.UrlManager.INDEX_CR;
import static com.android.murano500k.newradio.UrlManager.INDEX_CUSTOM;
import static com.android.murano500k.newradio.UrlManager.INDEX_DI;
import static com.android.murano500k.newradio.UrlManager.INDEX_JR;
import static com.android.murano500k.newradio.UrlManager.INDEX_RR;
import static com.android.murano500k.newradio.UrlManager.INDEX_RT;
import static junit.framework.Assert.assertTrue;



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
		urlManager=new UrlManager(context);
	}

	public boolean isShuffle() {
		boolean res = preferences.getBoolean(SHARED_PREFS_SHUFFLE, false);
		Log.d(TAG, "isShuffle " + res);
		return res;
	}

	public void saveActivePlaylistIndex(int index) {
		int was = getActivePlaylistIndex();
		preferences.edit().putInt(SHARED_PREFS_PLAYLIST_INDEX, index).apply();
		preferences.edit().apply();
		if (was != index) Log.d(TAG, "ActivePlaylistIndex " + index + " saved");
	}
	public int getActivePlaylistIndex() {

		int res = preferences.getInt(SHARED_PREFS_PLAYLIST_INDEX,
				INDEX_CUSTOM);
		Log.d(TAG, "getActivePlaylistIndex " + res);
		return res;
	}

	public void setShuffle(boolean shuffle) {
		boolean was = isShuffle();
		if (was != shuffle) {
			Log.d(TAG, "setShuffle " + shuffle + " saved");
			preferences.edit().putBoolean(SHARED_PREFS_SHUFFLE, shuffle).apply();
			preferences.edit().apply();
		}
	}


	public ArrayList<String> getStations() {
		return stationUrls;
	}

	public boolean selectPls(String plsString) {
		if(plsString == null) return false;
		M3U_Parser.M3UHolder holder;
		try {
			M3U_Parser parser=new M3U_Parser();
			holder = parser.parseString(plsString);
		} catch (Exception e) {
			return false;
		}
		if(holder==null) return false;
		else {
			assertTrue(saveStations(holder.getUrls(),holder.getNames()));
			setSelectedUrl(stationUrls.get(0));
		}
		return true;
	}

	private boolean saveStations(ArrayList<String> urls,ArrayList<String> names) {
		HashSet<String> arr = new HashSet<>();
		arr.addAll(urls);
		if(arr.size()>0) {
			preferences.edit().putStringSet(SHARED_PREFS_URLS, arr).apply();
			Log.d(TAG, "urls saved to prefs size " + arr.size());
			stationUrls=new ArrayList<>(arr);
		}else {
			Log.e(TAG, "urls NOT saved. size=" + arr.size());
			stationUrls=null;
			return false;
		}

			for (int i=0;i<names.size(); i++){
				String url = urls.get(i);
				String name = names.get(i);
				preferences.edit().putString(url, name).apply();
			}
		if(names.size()>0) {
			Log.d(TAG, "names saved to prefs size " + arr.size());
		}else {
			Log.e(TAG, "names NOT saved. size=" + arr.size());
			stationUrls=null;
			return false;
		}
		setSelectedUrl(stationUrls.get(0));
		return getSelectedUrl()!=null;
	}

	public String getSelectedUrl() {
		String urlSelected = preferences.getString(SHARED_PREFS_SELECTED_URL, null);
		if (urlSelected==null) {
			if(getStations()==null || getStations().size()==0) return null;
			else setSelectedUrl(getStations().get(0));
		}
		return urlSelected;
	}

	void setSelectedUrl(String url) {
		preferences.edit().putString(SHARED_PREFS_SELECTED_URL, url).apply();
	}

	public int getBufferSize() {
		return preferences.getInt(EXTRA_AUDIO_BUFFER_CAPACITY, 800);
	}

	void saveBufferSize(int size) {
		preferences.edit().putInt(EXTRA_AUDIO_BUFFER_CAPACITY, size).apply();
	}

	public int getDecodeSize() {
		return preferences.getInt(EXTRA_AUDIO_DECODE_CAPACITY, 800);
	}

	void saveDecodeSize(int size) {
		preferences.edit().putInt(EXTRA_AUDIO_DECODE_CAPACITY, size).apply();
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
		else return R.drawable.default_art;
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

	public String getActivePlaylistName() {
		int i = getActivePlaylistIndex();
		if (i < 0) return context.getResources().getString(R.string.app_name);
		else {
			switch (getActivePlaylistIndex()) {
				case INDEX_DI:
					return "Digitally Imported";
				case INDEX_JR:
					return "Jazz Radio";
				case INDEX_RR:
					return "Rock Radio";
				case INDEX_RT:
					return "Radio Tunes";
				case INDEX_CR:
					return "Classical radio";
				case INDEX_CUSTOM:
					return "Custom playlist";
			}
		}
		return context.getResources().getString(R.string.app_name);
	}
}





