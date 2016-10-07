package com.stc.radio.player.db;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.M3UParser;
import com.stc.radio.player.UrlManager;
import com.stc.radio.player.ui.MainActivity;
import com.stc.radio.player.ui.Metadata;
import com.stc.radio.player.ui.UiEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.UrlManager.DI;
import static com.stc.radio.player.UrlManager.JR;
import static com.stc.radio.player.UrlManager.RR;
import static com.stc.radio.player.UrlManager.RT;
import static com.stc.radio.player.UrlManager.getPlaylistUrl;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by artem on 9/21/16.
 */

public class DbHelper {
	private static final String TAG = "DbHelper";
	private static final String ACTIVE_PLAYLIST_NAME = "Active";
	private static final String CUSTOM_PLAYLIST_NAME = "Custom";

	public static long ID_PLAYLIST_DI;
	public static long ID_PLAYLIST_CR;
	public static long ID_PLAYLIST_RR;
	public static long ID_PLAYLIST_RT;
	public static long ID_PLAYLIST_JR;
	public static long ID_PLAYLIST_ACTIVE;
	public static long ID_PLAYLIST_CUSTOM;

	private static final String IMG_BASE_URL = "http://static.diforfree.org/img/channels/80/";

	public static void updateNowPlaying(UiEvent eventUi) {
		assertNotNull(eventUi.getExtras().url);
		String station = DbHelper.getNameFromUrl(eventUi.getExtras().url);
		UiEvent.UI_ACTION action =eventUi.getUiAction();
		int uiState= MainActivity.UI_STATE.IDLE;
		switch(action){
			case PLAYBACK_STARTED:
				uiState=MainActivity.UI_STATE.PLAYING;
				break;
			case PLAYBACK_STOPPED:
				uiState=MainActivity.UI_STATE.IDLE;
				break;
			case LOADING_STARTED:
				uiState=MainActivity.UI_STATE.LOADING;
				break;
		}
		DbHelper.setNowPlaying(
				DbHelper.getNowPlaying()
						.withUrl(eventUi.getExtras().url)
						.withUiState(uiState));
	}
	public static void updateNowPlaying(Metadata metadata) {
		DbHelper.setMetadata(metadata);
	}
	public static String getArtUrl(String url){
		String pls=null;

		if(url.contains("di.fm")) pls="di";
		else if(url.contains("rock")) pls="rockradio";
		else if(url.contains("jazz")) pls="jazzradio";
		else if(url.contains("tunes")) pls="radiotunes";
		else if(url.contains("classic")) pls="classicalradio";
		else return null;
		String station = url.substring(url.lastIndexOf("/")+1);
		station=station.substring(0, station.indexOf("?"));
		String result = IMG_BASE_URL+pls+"_"+station;
		if(url.contains("di.fm")) result+=".jpg";
		else result+="_hi.jpg";
		//Timber.v(result);
		return result;
	}


	public static NowPlaying getNowPlaying(){
		NowPlaying nowPlaying =new Select().from(NowPlaying.class).executeSingle();
		if(nowPlaying==null) {
			if(getPlaylistId(0)<0 && !checkIfPlaylistsExist()) createPlaylists();

			nowPlaying=new NowPlaying().withArtist(null).withSong(null).withActivePlaylistId(getPlaylistId(0)).withUiState(MainActivity.UI_STATE.IDLE).withUrl(null);
			nowPlaying.save();
		}
		return nowPlaying;
	}
	public static Station getCurrentStation(){
		NowPlaying nowPlaying =getNowPlaying();
		if(nowPlaying.url==null){
			if(!checkIfStationsExist()) createStations();
			Station station = new Select().from(Station.class).executeSingle();
			nowPlaying.withUrl(station.url).save();
			return station;
		}else {
			From test = new Select().from(Station.class).where("Url = ?", nowPlaying.getUrl());
			if(test.exists())
				return test.executeSingle();
		}
		fail("null");
		return new Station();
	}
	public static Playlist getCurrentPlaylist(){
		From from = new Select().from(Playlist.class).where("PlaylistId = ?", getNowPlaying().getActivePlaylistId());
		assertTrue(from.exists());
		return from.executeSingle();
	}

	public static void setActivePlaylistId(long playlistId) {
		getNowPlaying().withActivePlaylistId(playlistId).save();
	}


	public static int getCurrentPosition(){
		return getCurrentStation().position;
	}
	public static long getCurrentPlaylistId(){
		return getNowPlaying().getActivePlaylistId();
	}
	public static long setCurrentPlaylistId(){
		return getNowPlaying().getActivePlaylistId();
	}


	public static void setNowPlaying(NowPlaying newNowPlaying){
		new Delete().from(NowPlaying.class).execute();
		newNowPlaying.save();
	}
	public static NowPlaying setMetadata(Metadata metadata){
		NowPlaying nowPlaying = getNowPlaying();
		if(nowPlaying==null) nowPlaying=new NowPlaying();
		nowPlaying.withArtist(metadata.getArtist()).withSong(metadata.getSong()).save();
		return nowPlaying;
	}
	public static NowPlaying setUrl(String url){
		NowPlaying nowPlaying = getNowPlaying();
		if(nowPlaying==null) nowPlaying=new NowPlaying();
		nowPlaying.withUrl(url).save();
		return nowPlaying;
	}
	public static void updateActiveStations(List<Station> stations){
		ActiveAndroid.beginTransaction();
		try {
			for(Station station : stations) station.save();
			ActiveAndroid.setTransactionSuccessful();
		}catch (Exception e){
			Timber.e(e);
			throw new RuntimeException(e.getCause() );
		}
		finally {
			ActiveAndroid.endTransaction();
		}
	}

	public static NowPlaying setPlayerState(int state){
		NowPlaying nowPlaying = getNowPlaying();
		if(nowPlaying==null) nowPlaying=new NowPlaying();
		nowPlaying.withUiState(state).save();

			return nowPlaying;
	}

	public static long getPlaylistId(int position){
		Playlist playlist =new Select().from(Playlist.class).where("Position = ?", position).executeSingle();
		if(playlist==null) return -1;
		return playlist.getId();
	}

	public static long getPlaylistId(String name){
		return new Select().from(Playlist.class).where("Name = ?", name).executeSingle().getId();
	}
	public static String getPlaylistName(long id){
		Playlist playlist=new Select().from(Playlist.class).where("_id = ?", id).executeSingle();
		if(playlist==null) return "Radio";
		else return playlist.name;
	}
	public static String getPlaylistName(int  position){
		Playlist playlist=new Select().from(Playlist.class).where("Position = ?", position).executeSingle();
		if(playlist==null) return "Radio";
		else return playlist.name;
	}

	public static void setDecodeSize(int decodeSize) {
		checkSettingsExist();
		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		radioSettings.setDecodeSize(decodeSize);
		radioSettings.save();

		Timber.d("setDecodeSize %d saved", decodeSize);
	}
	public static void setBufferSize(int bufferSize) {
		checkSettingsExist();
		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		radioSettings.setBufferSize(bufferSize);
		radioSettings.save();

		Timber.d("setBufferSize %d saved", bufferSize);
	}
	public static void setShuffle(boolean shuffle) {
		checkSettingsExist();

		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		radioSettings.setShuffle(shuffle);
		radioSettings.save();
		Timber.d("setShuffle %b saved", shuffle);
	}
	public static int getDecodeSize() {
		checkSettingsExist();
		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		int res = radioSettings.getDecodeSize();
		//Timber.d("getDecodeSize %d",res);
		return res;
	}

	public static int getBufferSize() {
		checkSettingsExist();
		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		int res = radioSettings.getBufferSize();
		//Timber.d("getDecodeSize %d",res);
		return res;
	}
	public static boolean isShuffle() {
		checkSettingsExist();
		RadioSettings radioSettings = new Select().from(RadioSettings.class).executeSingle();
		boolean res = radioSettings.isShuffle();
		//Timber.d("isShuffle %b",res);
		return res;
	}
	public static void checkSettingsExist(){
		if(!new Select().from(RadioSettings.class).exists()) new RadioSettings(false,800,400).save();
	}

	public static List<Playlist> createPlaylists(){
		Timber.w("First launch init playlists");
		List<Playlist>list=new ArrayList<>();
		ActiveAndroid.beginTransaction();
		try {
			checkSettingsExist();
			list.add(new Playlist("Digitally Imported", getPlaylistUrl(DI), 0));
			list.add(new Playlist("Classical Radio", getPlaylistUrl(UrlManager.CR), 1));
			list.add(new Playlist("Jazz Radio", getPlaylistUrl(JR), 2));
			list.add(new Playlist("Rock Radio", getPlaylistUrl(RR), 3));
			list.add(new Playlist("Radio Tunes", getPlaylistUrl(RT), 4));
			for(Playlist p: list) p.save();
			ActiveAndroid.setTransactionSuccessful();
		}catch (Exception e){
			Timber.e(e);
			throw new RuntimeException(e.getCause() );
		}
		finally {
			ActiveAndroid.endTransaction();
		}
		return list;
	}
	public static boolean createStations(){
		Timber.w("First launch init stations");
		ActiveAndroid.beginTransaction();
		try {
			checkSettingsExist();
			List<Playlist> playlists=null;
			if(!checkIfPlaylistsExist()) playlists=createPlaylists();
			else playlists=new Select().from(Playlist.class).execute();

			for(Playlist p: playlists) {
				From from = new Select().from(Station.class).where("PlaylistId = ?", p.getId());
				if(!from.exists() || from.count()<1) downloadStations(p);
			}
			ActiveAndroid.setTransactionSuccessful();
		}catch (Exception e){
			Timber.e(e);
			throw new RuntimeException(e.getCause() );
		}
		finally {
			ActiveAndroid.endTransaction();
		}
		return true;
	}





	public static List<Station> getPlaylistStations(long ID) {
		if(!checkIfStationsExist()) createStations();
		return new Select()
				.from(Station.class)
				.where("PlaylistId = ?", ID)
				.execute();
	}
	public static  ArrayList<Station> downloadStations(Playlist playlist){
		return updatePlaylistStations(downloadDefaultPlaylistContent(playlist.url), playlist.getId());
	}

	public static String downloadDefaultPlaylistContent (String urlStr) {
		String res = "";
		String line;
		InputStream inputStream = null;
		Timber.d("downloadDefaultPlaylistContent");
		try {
				URL url = new URL(urlStr);
				inputStream = url.openStream();
			if (inputStream == null) {
				return null;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			while ((line = in.readLine()) != null) {
				res += line;
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getCause() );

		}
		if(res.length()<=0) return null;
		else return res;
	}
	public static ArrayList<Station> updatePlaylistStations(String plsString, long plsId) {
		//Timber.v("plsString :  %s", plsString);
		new Delete().from(Station.class).where("PlaylistId = ? ", plsId);
		ArrayList <Station> stations=new ArrayList<>();
		assertNotNull(plsString);
		assertTrue(plsString.length()>1);
		M3UParser.M3UHolder holder;
		try {
			M3UParser parser=new M3UParser();
			holder = parser.parseString(plsString);
		} catch (Exception e) {
			Timber.e( e.getMessage());
			throw new RuntimeException(e.getCause() );
		}
		if(holder==null) return stations;
			assertTrue(holder.getUrls().size()>0);
			assertTrue(holder.getNames().size()==holder.getUrls().size());
		for(int i=0;i<holder.getUrls().size()-1; i++){
			Station s=new Station(holder.getUrl(i), holder.getName(i), plsId);
			s.save();
			stations.add(s);
		}
		assertTrue(stations.size()>0);
		return stations;
	}
	public static  String getNameFromUrl(String url){
		Station station = new Select().from(Station.class).where("Url = ?", url).executeSingle();
		if(station!=null) return station.name;
		else return "Unnamed station";
	}


	public static boolean checkIfPlaylistsExist() {
		return new Select().from(Playlist.class).count()>4;
	}
	public static boolean checkIfStationsExist() {
		return new Select().from(Station.class).count()>1;
	}
	public static boolean checkDbContent() {
		return (new Select().from(Station.class).count()>1 && new Select().from(Playlist.class).count()>1 );
	}
	public static Observable<Integer> observeCheckDbContent() {
		return Observable.just(new Random().nextInt())
				.observeOn(Schedulers.computation())
				//.delay(3L, TimeUnit.SECONDS)//by default operates on computation Scheduler
				.doOnNext(new Action1<Integer>() {
					@Override
					public void call(Integer integer) {

						if(!checkIfPlaylistsExist()) createPlaylists();
						if(!checkIfStationsExist()) createStations();

						/*if (new Random().nextBoolean()) {
							//simulate error
							throw new RuntimeException("Error calculating value");
						}*/

					}
				});
	}

	public static void resetActiveStations() {
		List<Station> stations = new Select().from(Station.class).where("Active = ?", true).execute();
		if (stations != null) {
			for (Station station : stations) {
				station.active = false;
				station.position=-1;
				station.save();
			}
		}
	}
}

