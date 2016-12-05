package com.stc.radio.player.utils;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.db.DBMediaItem;
import com.stc.radio.player.db.DBUserPrefsItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by artem on 12/1/16.
 */

public class RatingHelper {

    static int maxPlayedTimes = 0;
    static public int getMaxPlayedTimes(){
        DBUserPrefsItem dbUserPrefsItem;
        From from = new Select ().from(DBUserPrefsItem.class);
        if(!from.exists()) {
            dbUserPrefsItem = new DBUserPrefsItem(0);
            dbUserPrefsItem.save();
        }else {
            dbUserPrefsItem = from.executeSingle();
        }
        return dbUserPrefsItem.getMaxPlayedTimes();
    }

    static public void setMaxPlayedTimes(int newValue){
        DBUserPrefsItem dbUserPrefsItem;
        From from = new Select ().from(DBUserPrefsItem.class);
        if(!from.exists()) {
            dbUserPrefsItem = new DBUserPrefsItem(0);
            dbUserPrefsItem.save();
        }else {
            dbUserPrefsItem = from.executeSingle();
        }
        dbUserPrefsItem.setMaxPlayedTimes(newValue);
        dbUserPrefsItem.save();

    }
/*
    static public float getUserRatingPercentageForItem(boolean isFavorite, int playedTimes){
        if(isFavorite) return 0.99f;
        if(maxPlayedTimes==0 || playedTimes==0) return
            rating= (float)playedTimes/(float)maxPlayedTimes;
        else rating=0.01f;
        Timber.w("getUserRatingPercentageForItem:%f",rating);
        if(rating==0.99f)return 0.9f;
        else return rating;
    }*/
    static public void incrementPlayedTimes(String mediaId){
        int newPlayedTimes=0;
        DBMediaItem dbMediaItem;
        From from = new Select ().from(DBMediaItem.class);
        if(!from.exists()){
            Timber.e("item %s not found. Can't increment played times"+mediaId);
            return;
        }else {
            dbMediaItem = from.executeSingle();
            newPlayedTimes=dbMediaItem.getPlayedTimes()+1;
            dbMediaItem.setPlayedTimes(newPlayedTimes);
            dbMediaItem.save();
        }
        Timber.w("%s incrementPlayedTimes(%d)", mediaId,newPlayedTimes);

        if(getMaxPlayedTimes()<newPlayedTimes) {
            Timber.w("setMaxPlayedTimes(%d)",newPlayedTimes);
            setMaxPlayedTimes(newPlayedTimes);
        }

    }
    static public void setFavorite(String mediaId, boolean isFav){
        DBMediaItem dbMediaItem;
        From from = new Select ().from(DBMediaItem.class);
        if(!from.exists()){
            Timber.e("item %s not found. Can't setFavorite"+mediaId);
            return;
        }else {
            dbMediaItem = from.executeSingle();
            dbMediaItem.setFavorite(isFav);
        }
        dbMediaItem.save();
    }
static public boolean updateFavorite(String mediaId){
        DBMediaItem dbMediaItem;
        From from = new Select ().from(DBMediaItem.class);
        if(!from.exists()){
            Timber.e("item %s not found. Can't setFavorite"+mediaId);
            return false;
        }else {
            dbMediaItem = from.executeSingle();
            dbMediaItem.setFavorite(!dbMediaItem.isFavorite());
        }
        dbMediaItem.save();
    return dbMediaItem.isFavorite();
    }

    static public boolean isFavorite(String mediaId) {
        DBMediaItem dbMediaItem;
        From from = new Select().from(DBMediaItem.class);
        if (!from.exists()) {
            Timber.e("item %s not found. Can't getFavorite" + mediaId);
            return false;
        } else {

            dbMediaItem = from.executeSingle();
            if(dbMediaItem.isFavorite())
                Timber.e("item %s found. isFavorite %b" , mediaId,dbMediaItem.isFavorite());
            return dbMediaItem.isFavorite();
        }
    }
    static public ArrayList<MediaMetadataCompat> sortByRating(ArrayList<MediaMetadataCompat> list){
        Collections.sort(list, new Comparator<MediaMetadataCompat> () {

            @Override
            public int compare(MediaMetadataCompat m1, MediaMetadataCompat m2) {
                boolean isFav1 = m1.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING).hasHeart();
                boolean isFav2 = m2.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING).hasHeart();
                if(isFav1) {
                    if(isFav2) return 0;
                    else return 1;
                } else {
                    if(isFav2) return -1;
                    else return 0;
                }
            }
        });
        return list;
    }

    static public List<DBMediaItem> sortByPlayedTimes(List<DBMediaItem> list){
        Collections.sort(list, new Comparator<DBMediaItem> (){
            @Override
            public int compare(DBMediaItem m1, DBMediaItem m2) {
                int r1=m1.getPlayedTimes();
                int r2=m2.getPlayedTimes();
                if(r1==r2) return 0;
                else if (r1>r2) return 1;
                else return -1;
            }
        });
        return list;
    }
}
