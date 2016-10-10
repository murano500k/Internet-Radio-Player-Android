package com.stc.radio.player.db;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.utils.M3UParser;
import com.stc.radio.player.utils.UrlManager;

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

import static com.stc.radio.player.utils.UrlManager.DI;
import static com.stc.radio.player.utils.UrlManager.JR;
import static com.stc.radio.player.utils.UrlManager.RR;
import static com.stc.radio.player.utils.UrlManager.RT;
import static com.stc.radio.player.utils.UrlManager.getPlaylistUrl;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

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
	private static final String IMG_BASE_PATH = "/sdcard";



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

	public static List<Playlist> createPlaylists(){
		Timber.w("First launch init playlists");
		List<Playlist>list=new ArrayList<>();
		ActiveAndroid.beginTransaction();
		try {
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
			Station s=new Station(holder.getUrl(i), holder.getName(i), plsId, getArtUrl(holder.getUrl(i)));
			s.save();
			stations.add(s);
		}
		assertTrue(stations.size()>0);
		if(NowPlaying.getInstance()==null) new NowPlaying().setStation(stations.get(0));
		else if( NowPlaying.getInstance().getStation()==null) NowPlaying.getInstance().setStation(stations.get(0));
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

