package com.stc.radio.player.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import static com.stc.radio.player.model.StationsManager.PLAYLISTS.DI;

/**
 * Created by artem on 10/12/16.
 */

public class SettingsProvider {
	public static final String SETTINGS_NAME = "com.stc.radio.player.SETTINGS_NAME";
	public static final String DB_EXISTS = "com.stc.radio.player.DB_EXISTS";
	public static final String AUTH_TOKEN = "com.stc.radio.player.AUTH_TOKEN";
	public static final String AUTH_EXPIRE_DATE = "com.stc.radio.player.AUTH_EXPIRE_DATE";
	public static final String ACTIVE_PLAYLIST = "com.stc.radio.player.ACTIVE_PLAYLIST";
	public static final String TIMER_MINUTES_LEFT = "com.stc.radio.player.TIMER_MINUTES_LEFT";
	public static SharedPreferences prefs;
	public static void init(Context c){
		if(prefs==null ) prefs= c.getSharedPreferences(SETTINGS_NAME,  Context.MODE_PRIVATE);
	}
	public static SharedPreferences getPrefs(){
		return prefs;
	}
	public static long getTokenExpires(){
		return prefs.getLong(AUTH_EXPIRE_DATE,100000);
	}
	public static String getToken(){
		return prefs.getString(AUTH_TOKEN,null);
	}


	public static  boolean isFirstRun(){
		return !prefs.contains(DB_EXISTS);
	}
	public static void setDbExistsTrue(){
		prefs.edit().putBoolean(DB_EXISTS, true).apply();
	}
	public static String getPlaylist()
	{
		return prefs.getString(ACTIVE_PLAYLIST, DI);
	}
	public static void setPlaylist(String  pls){

		prefs.edit().putString(ACTIVE_PLAYLIST, pls).apply();
	}
	public static void setTimerValueMinutes(int minutes){
		prefs.edit().putInt(TIMER_MINUTES_LEFT, minutes).apply();
	}
	//return isRunning
	public static boolean decrementTimerValueMinutes(){
		int currentValue=prefs.getInt(TIMER_MINUTES_LEFT, 0);
		if(currentValue<1) return false;
		else {
			prefs.edit().putInt(TIMER_MINUTES_LEFT, currentValue-1).apply();
			return true;
		}
	}
	public static int getTimerValueMinutes(){
		int currentValue=prefs.getInt(TIMER_MINUTES_LEFT, 0);
		Log.d("Settings", "getTimerValueMinutes: "+currentValue);
		if(currentValue<1) return 0;
		return currentValue;
	}






}
