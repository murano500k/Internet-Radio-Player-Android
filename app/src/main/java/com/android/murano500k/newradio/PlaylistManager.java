package com.android.murano500k.newradio;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;

/**
 * Created by artem on 8/9/16.
 */
public class PlaylistManager {
    private static final String TAG = "PlaylistManager";
    public static final String SHARED_PREFS_NAME = "com.android.murano500k.RadioPrefsFile";
    public static final String SHARED_PREFS_PLS_TYPE = "com.android.murano500k.SHARED_PREFS_PLS_TYPE";
    public static final String SHARED_PREFS_URL_SET = "com.android.murano500k.SHARED_PREFS_URL_SET";
    public static final String SHARED_PREFS_SELECTED_URL = "com.android.murano500k.SHARED_PREFS_SELECTED_URL";

    SharedPreferences preferences;
    static ArrayList<String> stations;
    static String urlSelected;

    public PlaylistManager(Context context) {
        this.preferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        stations=null;
        urlSelected=getSelectedUrl();
    }

    public boolean isOnlyFavorites(){
        boolean res=preferences.getBoolean(SHARED_PREFS_PLS_TYPE, false);
        //Log.d(TAG, "isOnlyFavorites " + res);
        return res;
    }

    public void setOnlyFavorites(boolean onlyFav){
        boolean was=isOnlyFavorites();
        if(was!=onlyFav){
            stations=null;
            Log.d(TAG, "setOnlyFavorites " + onlyFav+ " saved");
            preferences.edit().putBoolean(SHARED_PREFS_PLS_TYPE, onlyFav).apply();
            preferences.edit().apply();
        }
    }

    public Set<String> saveUrls(){
        Set<String> statsStrings =new HashSet<String>();
        for(String url: Constants.contentArr){
            statsStrings.add(url);
        }
        Log.d(TAG, "saveUrls  " + statsStrings.size()+ " saved");
        preferences.edit().putStringSet(SHARED_PREFS_URL_SET,statsStrings).apply();
        return statsStrings;
    }
    public ArrayList<String> getStations(){
        if(stations==null || stations.size()>0) {
            stations=new ArrayList<>();
            Set<String> statsStrings = preferences.getStringSet(SHARED_PREFS_URL_SET, new HashSet<String>());
            if(statsStrings.size()==0) statsStrings = saveUrls();
            for(String url: statsStrings){
                if(!isOnlyFavorites()) stations.add(url);
                else if(isStationFavorite(url)) stations.add(url);
            }
            if(stations.size()==0) {
             setOnlyFavorites(false);
                stations=getStations();
            }

            assert stations!=null;
            assertTrue(stations.size()>0);
        }
        return stations;
    }

    public void setStationFavorite(String url, boolean isFav){
        Log.d(TAG, "setStationFavorite " + url + " isFav " +isFav);

        if(isStationFavorite(url)==isFav)preferences.edit().putBoolean(url,isFav).apply();
    }
    public boolean isStationFavorite(String url){
        return preferences.getBoolean(url,false);
    }

    public String getSelectedUrl(){
        if(urlSelected==null) {
            urlSelected=preferences.getString(SHARED_PREFS_SELECTED_URL,
                    "ERROR");
            if(urlSelected.contains("ERROR")) {
                setSelectedUrl(getStations().get(0));
            }
        }
        return urlSelected;
    }
    public void setSelectedUrl(String url){
        preferences.edit().putString(SHARED_PREFS_SELECTED_URL,url).apply();
        if(urlSelected==null || !urlSelected.contains(url)){
            urlSelected=url;
        }
    }

    public int getIndex(String url){
        return getStations().indexOf(url);
    }

    public static String getNameFromUrl(String url){
        String name;
        if(url.contains("http") && url.contains("di_")){
            name = url.substring(url.lastIndexOf("di_")+3, url.lastIndexOf("_hi"));;
        }
        else name=url;
        Log.d(TAG, "getNameFromUrl: "+ name);
        return name;
    }
    public static String getArtistFromString(String data){
        String artistName="";
        if(data!=null && data.contains(" - ")) {
            artistName=data.substring(0, data.indexOf(" - "));
        }
        Log.d(TAG,"getArtistFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"artistNameStr: "+ artistName);

        return artistName;
    }
    public static String getTrackFromString(String data){
        String trackName="";
        if(data!=null && data.contains(" - ")) {
            trackName=data.substring(data.indexOf(" - ")+3);
        }
        Log.d(TAG,"getTrackFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"trackName: "+ trackName);
        return trackName;
    }
    public static int getArt(String fileName, Context c){
        int resID = c.getResources().getIdentifier(fileName, "drawable", c.getPackageName());
        if(resID!=0) return resID;
        else return R.drawable.default_art;
    }



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





