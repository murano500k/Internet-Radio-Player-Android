package com.stc.radio.player.contentmodel;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.StationListItem;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.SettingsProvider;

import java.io.IOException;
import java.util.ArrayList;
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

public class StationsManager {
	public static String getArtUrl(String s1) {
		return "http://somafm.com/img3/" + s1+ "-400.jpg";
	}
	public  static String getUrl(String playlist, String key) {
		int num=1;//new Random().nextInt(1)+1;
		if(playlist.equals(SOMA))
			return "http://ice" + num + ".somafm.com/" + key + "-128-aac";

		String base="";
		if(playlist.equals(DI) ) base+=playlist+"/"+key+"_hi";
		else if (playlist.equals(RADIOTUNES) || playlist.equals(ROCK))base+="radiotunes.com"+"/"+key+"_hi";
		else base+=playlist+".com"+"/"+key;
		return "http://prem"+num+"."+base+"?"+ SettingsProvider.getToken();
	}
	/**
	 * Created by artem on 10/13/16.
	 */

	public static class PLAYLISTS {
		public static final String DI="di.fm";
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
						Timber.w("first check if pls in db: %s",pls);
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
						else Timber.e("request pls error");

						throw new RuntimeException("ERROR download pls failed");
					}
				}).flatMap(new Func1<List<Station>, Observable<Station>>() {
					@Override
					public Observable<Station> call(List<Station> stations) {
						if (stations != null) {

							Timber.w("downloaded pls size %d",stations.size());
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
						StationListItem listItem = new StationListItem().withStation(station);
						return Observable.just(listItem);
					}
				});
	}
}
