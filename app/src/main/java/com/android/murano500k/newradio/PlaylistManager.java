package com.android.murano500k.newradio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;

/**
 * Created by artem on 8/9/16.
 */
public class PlaylistManager {
	public static final String TAG = "PlaylistManager";

	public static final String SHARED_PREFS_NAME = "com.android.murano500k.RadioPrefsFile";
	public static final String SHARED_PREFS_PLS_TYPE = "com.android.murano500k.SHARED_PREFS_PLS_TYPE";
	public static final String SHARED_PREFS_SHUFFLE = "com.android.murano500k.SHARED_PREFS_PLS_TYPE";
	public static final String SHARED_PREFS_URLS_ALL = "com.android.murano500k.SHARED_PREFS_URLS_ALL";
	public static final String SHARED_PREFS_URLS_FAV = "com.android.murano500k.SHARED_PREFS_URLS_FAV";
	public static final String SHARED_PREFS_SELECTED_URL = "com.android.murano500k.SHARED_PREFS_SELECTED_URL";

	private SharedPreferences preferences;
	private static Set<String> stationsAll;
	private static Set<String> stationsFav;

	private static String urlSelected;
	Context context;

	private static String logString = "";

	public PlaylistManager(Context context) {
		this.context=context;
		this.preferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		stationsAll = null;
		stationsFav = null;
		urlSelected = getSelectedUrl();
	}

	public boolean isShuffle() {
		boolean res = preferences.getBoolean(SHARED_PREFS_SHUFFLE, false);
		Log.d(TAG, "isShuffle " + res);
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

	public boolean isOnlyFavorites() {
		boolean res = preferences.getBoolean(SHARED_PREFS_PLS_TYPE, false);
		//Log.d(TAG, "isOnlyFavorites " + res);
		return res;
	}

	public void setOnlyFavorites(boolean onlyFav) {
		boolean was = isOnlyFavorites();
		if (was != onlyFav) {
			Log.d(TAG, "setOnlyFavorites " + onlyFav + " saved");
			preferences.edit().putBoolean(SHARED_PREFS_PLS_TYPE, onlyFav).apply();
			preferences.edit().apply();
		}
	}

	public Set<String> loadUrls(String type) {

		Set<String> arr = preferences.getStringSet(type, new HashSet<String>());
		if (arr.size() < 1 && type.contains(SHARED_PREFS_URLS_ALL)) {
			arr = new HashSet<String>();
			for (String url : Constants.contentArr) {
				arr.add(url);
			}
			preferences.edit().putStringSet(type, arr).apply();
			Log.d(TAG, "stations saved to prefs size " + arr.size());
		}
		Log.d(TAG, "stations loaded size " + arr.size() + " " + type);
		return arr;
	}
	public void addStation(String url, boolean fav) {
		Log.d(TAG, "addStation " + url);
		if (fav) {
			if(stationsFav==null) stationsFav=loadUrls(SHARED_PREFS_URLS_FAV);
			stationsFav.add(url);
			preferences.edit().putStringSet(SHARED_PREFS_URLS_FAV, stationsFav).apply();
		} else {
			if(stationsAll==null || stationsAll.size()<1) stationsAll=loadUrls(SHARED_PREFS_URLS_ALL);
			stationsAll.add(url);
			preferences.edit().putStringSet(SHARED_PREFS_URLS_ALL, stationsAll).apply();
		}
		stationsAll=null;
		stationsFav=null;
		getStations();

		Intent intent=new Intent(context, ServiceRadio.class);
		intent.setAction(Constants.INTENT.UPDATE_STATIONS);
		context.startService(intent);
	}

	public HashSet<String> getStations() {
		HashSet<String> stationsResult = new HashSet<>();
		if (stationsAll == null || stationsAll.size() < 1) {
			stationsAll = loadUrls(SHARED_PREFS_URLS_ALL);
			assertTrue(stationsAll.size() > 0);
		}
		if (stationsFav == null) stationsFav = loadUrls(SHARED_PREFS_URLS_FAV);
		stationsResult.addAll(stationsFav);

		if (!isOnlyFavorites()) stationsResult.addAll(stationsAll);
		Log.d(TAG, "getStations() isOnlyFavorites=" + isOnlyFavorites() + " size=" + stationsResult.size());
		return stationsResult;
	}

	public void setStationFavorite(String url, boolean isFav) {
		Log.d(TAG, "setStationFavorite " + url + " isFav " + isFav);
		getStations();
		if (stationsAll == null || stationsAll.size() < 1) getStations();
		if (isFav) {
			if (stationsAll.remove(url)) stationsFav.add(url);
		} else {
			if (stationsFav.remove(url)) stationsAll.add(url);
		}
		preferences.edit().putStringSet(SHARED_PREFS_URLS_ALL, stationsAll).apply();
		preferences.edit().putStringSet(SHARED_PREFS_URLS_FAV, stationsFav).apply();


	}

	public boolean isStationFavorite(String url) {
		return (stationsFav != null && stationsFav.contains(url));
	}

	public String getSelectedUrl() {
		if (urlSelected == null) {
			urlSelected = preferences.getString(SHARED_PREFS_SELECTED_URL,
					"ERROR");
			if (urlSelected.contains("ERROR")) {
				setSelectedUrl(getStations().iterator().next());
			}
		}
		return urlSelected;
	}

	public void setSelectedUrl(String url) {
		preferences.edit().putString(SHARED_PREFS_SELECTED_URL, url).apply();
		if (urlSelected == null || !urlSelected.contains(url)) {
			urlSelected = url;
		}
	}

	public int getBufferSize() {
		return preferences.getInt(Constants.DATA_AUDIO_BUFFER_CAPACITY, 800);
	}

	public void saveBufferSize(int size) {
		preferences.edit().putInt(Constants.DATA_AUDIO_BUFFER_CAPACITY, size).apply();
	}

	public int getDecodeSize() {
		return preferences.getInt(Constants.DATA_AUDIO_DECODE_CAPACITY, 800);
	}

	public void saveDecodeSize(int size) {
		preferences.edit().putInt(Constants.DATA_AUDIO_DECODE_CAPACITY, size).apply();
	}


	public static String getNameFromUrl(String url) {
		String name;
		if (url.contains("http") && url.contains("di_")) {
			name = url.substring(url.lastIndexOf("di_") + 3, url.lastIndexOf("_hi"));
		} else if(url.contains("http://") || url.contains("https://") ){
			name = url.substring(url.lastIndexOf("://") + 3);
		}else name = url;
		//Log.d(TAG, "getNameFromUrl: " + name);
		return name;
	}

	public static String getArtistFromString(String data) {
		String artistName = "";
		if (data != null && data.contains(" - ")) {
			artistName = data.substring(0, data.indexOf(" - "));
		}/*
		Log.d(TAG, "getArtistFromString");
		Log.d(TAG, "dataStr: " + data);
		Log.d(TAG, "artistNameStr: " + artistName);*/

		return artistName;
	}

	public static String getTrackFromString(String data) {
		String trackName = "";
		if (data != null && data.contains(" - ")) {
			trackName = data.substring(data.indexOf(" - ") + 3);
		}/*
		Log.d(TAG, "getTrackFromString");
		Log.d(TAG, "dataStr: " + data);
		Log.d(TAG, "trackName: " + trackName);*/
		return trackName;
	}

	public static int getArt(String fileName, Context c) {
		int resID = c.getResources().getIdentifier(fileName, "drawable", c.getPackageName());
		if (resID != 0) return resID;
		else return R.drawable.default_art;
	}


	public static String getLog() {
		return logString;

	}


	public static void addToLog(String logString1) {
		logString += "\n>"+logString1;
	}

/*
public void savePrefsToStorage(boolean exitOnCompleted){
        Log.d(TAG, "savePrefsToStorage");

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {


                SharedPreferences.Editor editor = getSharedPreferences(
                        Constants.SHARED_PREFS_NAME, MODE_PRIVATE).edit();

SharedPreferences.Editor editor = preferences.edit();
editor.putBoolean(Constants.SHARED_PREFS_IS_FAV, isFavOnly());

        Set<String> statsStrings =new HashSet<String>();
        for(Station s: stations){
        if(s.fav) {
        Log.d(TAG, "write prefs: " +  s.url);
        statsStrings.add(s.url);
        }
        }

        editor.putStringSet(Constants.SHARED_PREFS_FAV_STATIONS_SET, statsStrings);
        if(editor.commit()) subscriber.onCompleted();
        }
        }).subscribeOn(Schedulers.newThread())
        .observeOn(Schedulers.newThread())
        .doOnCompleted(() -> {
        Log.d(TAG, "exitOnCompleted: " +  exitOnCompleted );

        if(exitOnCompleted){
        Log.d(TAG, "stopSelf");
        stopSelf();
        }
        })
        .subscribe();
        }
    public boolean getPrefsFromStorage(){
        boolean res=false;

        if(preferences!=null) {

            setFavOnly(preferences.getBoolean(Constants.SHARED_PREFS_IS_FAV, false));
            Log.d(TAG, "Read prefs: favOnly="+isFavOnly());

            Set<String> statsStrings = preferences.getStringSet(Constants.SHARED_PREFS_FAV_STATIONS_SET, new HashSet<String>());
            stationsFav=new ArrayList<>();
            Log.d(TAG, "Read prefs: statsStrings.size="+statsStrings.size());
            Log.d(TAG, "stations.size()="+ stations.size());

            if(statsStrings.size()!=0){
                res=true;
                for(String s: statsStrings){
                    Log.d(TAG, "fav station loaded "+ s);

                    Station station= getStationByUrl(s);
                    Log.d(TAG, "station == null -> "+ (station==null));

                    stationsFav.add(station);
                    stations.get(stations.indexOf(station)).fav=true;

                }
            }
        }
        getStationsListChangedNotification();
        return res;
    }
    public static ArrayList<Station> initStations(){
        ArrayList<Station>list = new ArrayList<>();

        for (int i = 0; i < contentArr.length; i++) {
            list.add(createStationItem(i, contentArr[i]));
        }
        return list;
    }
    private static Station createStationItem(int position,String urlStr) {
        return new Station(position, makeNameFromUrl(urlStr),urlStr);
    }

    */

}





