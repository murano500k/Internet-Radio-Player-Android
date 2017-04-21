package com.stc.radio.player.utils;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

/**
 * Created by artem on 4/21/17.
 */

public class ListColumnsCounter {

    private static final int DEFAULT_COLUMN_WIDTH = 120;
    private static final int LARGE_COLUMN_WIDTH = 180;
    private static final boolean DEFAULT_USE_LARGE_COLUMNS=false;
    private static final String PREFS_KEY_USE_LARGE_COLUMNS = "PREFS_KEY_USE_LARGE_COLUMNS";


    public static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (dpWidth / getMinColumnWidth(context));
    }

    public static int getMinColumnWidth(Context context){
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_KEY_USE_LARGE_COLUMNS, DEFAULT_USE_LARGE_COLUMNS)){
            return LARGE_COLUMN_WIDTH;
        }else return DEFAULT_COLUMN_WIDTH;
    }
    public static void setUseLargeColumns(Context context, boolean value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREFS_KEY_USE_LARGE_COLUMNS, value).apply();
    }

    public static boolean isUsingLargeColumns(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_KEY_USE_LARGE_COLUMNS, DEFAULT_USE_LARGE_COLUMNS);
    }


}
