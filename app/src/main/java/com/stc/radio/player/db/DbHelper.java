package com.stc.radio.player.db;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;

import java.util.ArrayList;
import java.util.List;

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



/*

	public static String getArtUrl(String url){
		String name=null;

		if(url.contains("di.fm")) name="di";
		else if(url.contains("rock")) name="rockradio";
		else if(url.contains("jazz")) name="jazzradio";
		else if(url.contains("tunes")) name="radiotunes";
		else if(url.contains("classic")) name="classicalradio";
		else return null;
		String station = url.substring(url.lastIndexOf("/")+1);
		station=station.substring(0, station.indexOf("?"));
		String result = IMG_BASE_URL+name+"_"+station;
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
			//s.save();

			stations.add(s);
		}

		assertTrue(stations.size()>0);
		return stations;
	}




	public static  String getNameFromUrl(String url){
		Station station = new Select().from(Station.class).where("Url = ?", url).executeSingle();
		if(station!=null) return station.getName();
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

						*//*if (new Random().nextBoolean()) {
							//simulate error
							throw new RuntimeException("Error calculating value");
						}*//*

					}
				});
	}*/

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


	public static void trannsformToStations(List <Station> stations) {
		ActiveAndroid.beginTransaction();
		try{
			From from=new Select().from(Station.class).where("Active = ?", true);
			if(from.exists()) {
				List<Station> stationsOld =from.execute();
				if (stationsOld != null) {
					for (Station station : stationsOld) {
						station.setActive(false);
						station.save();
					}
				}
			}
			for(int i = 0;i<stations.size();i++)  {
				stations.get(i).setActive(true);
				stations.get(i).setPosition(i);
				stations.get(i).save();
			}
			ActiveAndroid.setTransactionSuccessful();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			ActiveAndroid.endTransaction();
		}
	}
}

