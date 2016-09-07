package com.android.murano500k.newradio;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.odesanmi.m3ufileparser.M3U_Parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;

/**
 * Created by artem on 8/9/16.
 */
public class PlaylistManager {
	public static final String TAG = "PlaylistManager";

	public static final String SHARED_PREFS_NAME = "com.android.murano500k.SHARED_PREFS_NAME";
	public static final String SHARED_PREFS_SHUFFLE = "com.android.murano500k.SHARED_PREFS_SHUFFLE";
	public static final String SHARED_PREFS_URLS = "com.android.murano500k.SHARED_PREFS_URLS";
	public static final String SHARED_PREFS_NAMES = "com.android.murano500k.SHARED_PREFS_NAMES";
	public static final String SHARED_PREFS_SELECTED_URL = "com.android.murano500k.SHARED_PREFS_SELECTED_URL";
	//public static final String SHARED_PREFS_M3U_FILE = "com.android.murano500k.SHARED_PREFS_M3U_FILE";

	private SharedPreferences preferences;

	private static ArrayList<String> stationUrls=null;

	Context context;


	public PlaylistManager(Context context) {
		this.context=context;
		this.preferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		getStations();
		getSelectedUrl();
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


	private ArrayList<String> loadUrls() {
		Set<String> arr = preferences.getStringSet(SHARED_PREFS_URLS, null);
		if(arr==null)return null;
		else return new ArrayList<>(arr);
	}



	public ArrayList<String> getStations() {
		if(stationUrls==null) {
			Log.w(TAG, "getStations() local list is empty, try loadUrls");
			stationUrls = loadUrls();
			if (stationUrls == null || stationUrls.size() == 0) {
				Log.e(TAG, "loadUrls failed");
				return new ArrayList<>();
			}
		}
		return stationUrls;
	}

	public String selectPls(Uri uri) {
		String res="";
		if(uri == null) return "empty";
		try {
			InputStream inputStream =context.getContentResolver().openInputStream(uri);
			if(inputStream==null) return "null";
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ((line = br.readLine()) != null) {
				res+=line;
			}
			br.close();
			Log.w(TAG, "res: "+res);

		} catch (FileNotFoundException e) {
			return e.getMessage();
		} catch (IOException e) {
			return e.getMessage();
		}
		M3U_Parser.M3UHolder holder;

		try {
			M3U_Parser parser=new M3U_Parser();
			holder = parser.parseString(res);
		} catch (Exception e) {
			return e.getMessage();
		}
		if(holder==null) return "empty";
		else {
			assertTrue(saveStations(holder.getUrls(),holder.getNames()));
		}
		return null;
	}

	private boolean saveStations(ArrayList<String> urls,ArrayList<String> names) {
		HashSet<String> arr = new HashSet<String>();
		for (String url : urls) arr.add(url);
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

	public void setSelectedUrl(String url) {
		preferences.edit().putString(SHARED_PREFS_SELECTED_URL, url).apply();
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


	public String getNameFromUrl(String requestUrl) {
		return preferences.getString(requestUrl, "station");
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

	public int getArt(String url, Context c) {
		int resID = c.getResources().getIdentifier(getNameFromUrl(url), "drawable", c.getPackageName());
		if (resID != 0) return resID;
		else return R.drawable.default_art;
	}




	/*public void addStation(String url, boolean fav) {
			Log.d(TAG, "addStation " + url);
			if(stationUrls==null || stationUrls.size()<1) stationUrls=loadUrls(SHARED_PREFS_URLS_ALL);
			stationUrls.add(url);
			preferences.edit().putStringSet(SHARED_PREFS_URLS_ALL, stationUrls).apply();
			if (fav) {
				setStationFavorite(url, true);
			}
			stationUrls=null;
			getStations();
			Intent intent=new Intent(context, ServiceRadioRx.class);
			intent.setAction(ActivityRxTest.INTENT_UPDATE_STATIONS);
			context.startActivity(intent);
		}*/
	public void setStationFavorite(String url, boolean isFav) {
		/*Log.d(TAG, "setStationFavorite " + url + " isFav " + isFav);
		if (stationUrls == null || stationUrls.size() < 1) getStations();
		Set<String> stationsFav=loadUrls(SHARED_PREFS_URLS_FAV);
		if (isFav && !stationsFav.contains(url)) stationsFav.add(url);
		else if(!isFav && stationsFav.contains(url))  stationsFav.remove(url);
		preferences.edit().putStringSet(SHARED_PREFS_URLS_FAV, stationsFav).apply();
		stationsFav=null;
		getStations();*/
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

	public boolean isOnlyFavorites() {
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





