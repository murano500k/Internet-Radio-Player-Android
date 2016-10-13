package com.stc.radio.player.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by artem on 10/12/16.
 */

public class SettingsProvider {
	public static final String SETTINGS_NAME = "com.stc.radio.player.SETTINGS_NAME";
	public static final String DB_EXISTS = "com.stc.radio.player.DB_EXISTS";
	public static final String AUTH_TOKEN = "com.stc.radio.player.AUTH_TOKEN";
	public static final String AUTH_EXPIRE_DATE = "com.stc.radio.player.AUTH_EXPIRE_DATE";
	public static final String ACTIVE_PLAYLIST_NAME = "com.stc.radio.player.DB_EXISTS";
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
	public static void setDbExistsTruef(){
		prefs.edit().putBoolean(DB_EXISTS, true).apply();
	}


}
