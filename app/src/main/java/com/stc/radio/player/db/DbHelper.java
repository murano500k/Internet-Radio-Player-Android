package com.stc.radio.player.db;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by artem on 9/21/16.
 */

public class DbHelper {

	public static boolean isShuffle(){
		boolean res=false;
		From from=new Select().from(DBUserPrefsItem.class);
		if(from.exists()) res=((DBUserPrefsItem)from.executeSingle()).isShuffle();
		Timber.w("isShuffle: %b",res);
		return res;
	}


	public static void setShuffle(boolean shuffle){
		From from=new Select().from(DBUserPrefsItem.class);
		DBUserPrefsItem item=null;
		if(from.exists()) item=from.executeSingle();
		else item=new DBUserPrefsItem();
		item.setShuffle(shuffle);
		Timber.w("isShuffle: %b",shuffle);
		item.save();
	}

	public static List<Station> trannsformToStations(List <ParsedPlaylistItem> parsedPlaylistItemList, String pls) {
		List<Station> newlist=new ArrayList<Station>();
		ActiveAndroid.beginTransaction();
		try{

			for(int i = 0; i< parsedPlaylistItemList.size(); i++)  {
				Station station =new Station(parsedPlaylistItemList.get(i), pls, i,true);
				station.save();
				newlist.add(station);
			}
			ActiveAndroid.setTransactionSuccessful();
		 }catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			ActiveAndroid.endTransaction();
		}

		return newlist;
	}


}

