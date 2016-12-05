package com.stc.radio.player.contentmodel;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.StationListItem;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.model.MusicProviderSource;
import com.stc.radio.player.utils.SettingsProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.DI;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.RADIOTUNES;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.ROCK;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.SOMA;

/**
 * Created by artem on 10/15/16.
 */

public abstract class StationsManager  implements MusicProviderSource {
	public static String getArtUrl(String s1) {
		return "http://somafm.com/img3/" + s1+ "-400.jpg";
	}
	public static String getArtUrl(ParsedPlaylistItem item){
		String s = item.getImages().getDefault();
		//Timber.w("arturl=%s",s);
		if(s==null) Timber.e("ERROR null");
		else return "http:"+s.replace("{?size,height,width,quality,pad}", "?size=200x200");
		return "null";
	}
	public  static String getUrl(String playlist, String key) {
		int num=1;//new Random().nextInt(1)+1;
		if(playlist.contains(SOMA))
			return "http://ice" + num + ".somafm.com/" + key + "-128-aac";

		String base="";
		if(playlist.equals(DI) ) base+="di.fm"+"/"+key+"_hi";
		else if (playlist.equals(RADIOTUNES) )base+="radiotunes.com"+"/"+key+"_hi";
		else if ( playlist.equals(ROCK))base+="rockradio.com"+"/"+key;
		else base+=playlist+".com"+"/"+key;
		return "http://prem"+num+"."+base+"?"+ SettingsProvider.getToken();
	}

	@Override
	public Iterator<MediaMetadataCompat> iterator() {
		return null;
	}

	/**
	 * Created by artem on 10/13/16.
	 */

	public static class PLAYLISTS {
		public static final String DI="difm";
		public static final String RADIOTUNES="radiotunes";
		public static final String JAZZ="jazzradio";
		public static final String ROCK="rockradio";
		public static final String CLASSIC="classicalradio";
		public static final String FAV="favorite";
		public static final String SOMA="somafm";
	}

	/**
	 * Created by artem on 10/13/16.
	 */


	public static class Soma extends Station {


		public static final String[] somaStations = {
				"groovesalad",
				"dronezone",
				"spacestation",
				"secretagent",
				"lush",
				"u80s",
				"deepspaceone",
				"beatblender",
				"defcon",
				"seventies",
				"folkfwd",
				"bootliquor",
				"suburbsofgoa",
				"poptron",
				"thistle",
				"fluid",
				"digitalis",
				"illstreet",
				"7soul",
				"brfm",
				"cliqhop",
				"doomed",
				"earwaves",
		};
	}
	public static void updateDB() {
		Retro.updateToken();
		SettingsProvider.getToken();
		String[] playlists = {
				PLAYLISTS.DI,
				PLAYLISTS.CLASSIC,
				PLAYLISTS.JAZZ,
				PLAYLISTS.ROCK,
				PLAYLISTS.RADIOTUNES,
				PLAYLISTS.SOMA


		};
		List<Station> list;
		for (String pls : playlists) {
			From from = new Select().from(Station.class).where("Playlist = ?", pls);
			if (from.exists()) {
				list = from.execute();
				if(list!=null && !list.isEmpty()) Timber.w("SUCCESS: %s", pls);
			} else if (pls.contains(SOMA)) {
				int i = 0;
				list = new ArrayList<>();
				ActiveAndroid.beginTransaction();
				try{
					for (String s1 : StationsManager.Soma.somaStations) {
						Station station = new Station(s1, s1, s1,
								StationsManager.getArtUrl(s1), pls, i, true);
						Timber.w("Soma : %s", station.toString());
						station.save();
						list.add(station);
						i++;
					}
					ActiveAndroid.setTransactionSuccessful();
				}catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					ActiveAndroid.endTransaction();
				}

			} else {
				Response<List<ParsedPlaylistItem>> response = null;
				Call<List<ParsedPlaylistItem>> loadSizeCall;
				if (pls.equals(DI) || pls.equals("di.fm"))
					loadSizeCall = Retro.getStationsCall("di");
				else loadSizeCall = Retro.getStationsCall(pls);
				try {
					response = loadSizeCall.execute();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if (response != null && response.isSuccessful() && !response.body().isEmpty()) {
					list=new ArrayList<Station>();
					ActiveAndroid.beginTransaction();
					try{

						for(int i = 0; i< response.body().size(); i++)  {
							Station station =new Station(response.body().get(i), pls, i,true);
							station.save();
							list.add(station);
						}
						ActiveAndroid.setTransactionSuccessful();
					}catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						ActiveAndroid.endTransaction();
					}
				}
			}
		}

	}
	public static List<Station> getAll() {
		return new Select().from(Station.class).execute();
	}


		public static rx.Observable<StationListItem> getPlsUpdateObservable(String pls){
		return Observable.just(Retro.hasValidToken()).observeOn(Schedulers.io())
				.subscribeOn(Schedulers.newThread()).flatMap(new Func1<Boolean, Observable<String>>() {
					@Override
					public Observable<String> call(Boolean aBoolean) {
						if(aBoolean) return Observable.just(SettingsProvider.getToken());
						else  return Observable.just(Retro.updateToken());
					}
				}).flatMap(new Func1<String, Observable<String>>() {
					@Override
					public Observable<String> call(String s) {
						Timber.w("Token = %s", s);
						return Observable.just(pls);
					}
				}).flatMap(new Func1<String, Observable<List<Station>>>() {
					@Override
					public Observable<List<Station>> call(String s) {
						List<Station>list = new ArrayList<Station>();
						Timber.w("first check if name in db: %s",pls);
						From from;
						if(pls.contains("favorite")){
							from=new Select().from(Station.class).where("Favorite = ?", true);
							if(!from.exists()) {
								Timber.w("fav selected, but empty");
								from = new Select().from(Station.class).where("Playlist = ?", StationsManager.PLAYLISTS.DI);
							}else {
								list=from.execute();
								return Observable.just(list);
							}
						}else from = new Select().from(Station.class).where("Playlist = ?", s);
						if (from.exists()) {
							list = from.execute();
						}else if(pls.contains(SOMA)) {
							int i=0;
							for (String s1: StationsManager.Soma.somaStations){
								Station station = new Station(s1, s1, s1,
										StationsManager.getArtUrl(s1),pls,  i ,true);
								Timber.w("Soma : %s", station.toString());
								station.save();
								list.add(station);
								i++;
							}
						}else if(pls.contains("favorite")){
							list=new ArrayList<Station>();
						}
						//DbHelper.trannsformToStations(list);
						if (list.size() > 1 || pls.contains("favorite")) return Observable.just(list);
						else{
							throw new RuntimeException("ERROR Stations not found");
						}
					}
				}).onErrorResumeNext(new Func1<Throwable, Observable<? extends List<Station>>>() {
					@Override
					public Observable<? extends List<Station>> call(Throwable throwable) {
						Timber.w("Check db error: %s", throwable.getMessage());
						Response<List<ParsedPlaylistItem>> response = null;
						Call<List<ParsedPlaylistItem>> loadSizeCall;
						if(pls.equals(DI)||pls.equals("di.fm"))
							loadSizeCall = Retro.getStationsCall("di");
						else loadSizeCall = Retro.getStationsCall(pls);
						try {
							response = loadSizeCall.execute();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}

						if (response != null && response.isSuccessful() && !response.body().isEmpty()) {
							List<Station> stations = DbHelper.trannsformToStations(response.body(), pls);
							return Observable.just(stations);
						}
						else Timber.e("request name error");

						throw new RuntimeException("ERROR download name failed");
					}
				}).flatMap(new Func1<List<Station>, Observable<Station>>() {
					@Override
					public Observable<Station> call(List<Station> stations) {
						if (stations != null) {

							Timber.w("downloaded name size %d",stations.size());
							NowPlaying nowPlaying= NowPlaying.getInstance();
							if(nowPlaying==null ) nowPlaying=new NowPlaying();
							if(nowPlaying.getStation()==null && stations.size()>0 && !stations.contains(nowPlaying.getStation()))
								nowPlaying.setStation(stations.get(0), false);
							if( stations.size()>0) nowPlaying.setStations(stations, true);
							nowPlaying.save();
						} else throw new RuntimeException("ERROR no stations in list");
						return Observable.from(stations);
					}
				}).observeOn(Schedulers.newThread()).subscribeOn(Schedulers.computation()).flatMap(new Func1<Station, Observable<StationListItem>>() {
					@Override
					public Observable<StationListItem> call(Station station) {
						StationListItem listItem = new StationListItem(station);
						return Observable.just(listItem);
					}
				});
	}
}
