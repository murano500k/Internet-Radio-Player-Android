package com.stc.radio.player;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;
import com.stc.radio.player.contentmodel.Retro;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.PabloPicasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.activeandroid.Cache.getContext;

public class LoadingActivity extends AppCompatActivity {
	ProgressBar progressBar;
	private TextView status;
	ImageView imageView;
	Subscription checkDbSubscription;
	int counter;
	private int size;
	List<Station> allStations;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading);
		progressBar = (ProgressBar) findViewById(R.id.progressDB);
		status = (TextView) findViewById(R.id.textStatus);
		imageView = (ImageView) findViewById(R.id.imageView);
		status.setText("loading started");


		Observable.just(Retro.hasValidToken()).observeOn(Schedulers.newThread()).subscribeOn(Schedulers.newThread()).subscribe(aBoolean -> {
			if(!aBoolean) Timber.w("token %s",Retro.updateToken());
		});

		String[] playlists = {
				"jazzradio",
				"rockradio",
				"classicalradio",
				"radiotunes",
				"di"
		};
		allStations=new ArrayList<>();
		Observable.from(playlists).observeOn(Schedulers.newThread()).subscribeOn(Schedulers.computation()).delay(3000, TimeUnit.MILLISECONDS).doOnCompleted(() -> {
			Timber.w("fin");
			ActiveAndroid.beginTransaction();
			try{
				new Delete().from(Station.class).execute();
				for(Station S:allStations) S.save();
				ActiveAndroid.setTransactionSuccessful();
			}catch (Exception er){
				er.printStackTrace();
			}finally {
				ActiveAndroid.endTransaction();
			}
			/*runOnUiThread(() -> {

				Station station = allStations.get(0);
				assertNotNull(station);
				Timber.w("art: ",station.getArtUrl());
				PabloPicasso.with(getContext()).load(station.getArtUrl()).error(R.drawable.default_art).fit().into(imageView);
				progressBar.setVisibility(View.GONE);
			});*/
		}).subscribe(this::loadPlaylistStations);
	}



		private void loadPlaylistStations(String pls) {
			Timber.w("name=%s",pls);
			Call<List<ParsedPlaylistItem>> loadSizeCall = Retro.getStationsCall(pls);
			loadSizeCall.enqueue(new Callback<List<ParsedPlaylistItem>>() {
				@Override
				public void onResponse(Call<List<ParsedPlaylistItem>> call, Response<List<ParsedPlaylistItem>> response) {
					for(ParsedPlaylistItem item: response.body()) {

						Timber.w("%s",item.getKey());
						Station s = new Station(item, pls,1,true);
						allStations.add(s);
						if(allStations.size()==50) PabloPicasso.with(getContext()).load(s.getArtUrl()).error(R.drawable.default_art).fit().into(imageView);
						//progressBar.setVisibility(View.GONE);
						Timber.w("station= %s", s.toString());
						Timber.w("url= %s", s.getUrl());
						Timber.w("ArtUrl= %s", s.getArtUrl());
					}

				}

				@Override
				public void onFailure(Call<List<ParsedPlaylistItem>> call, Throwable t) {
					Timber.e("%s",t.toString());
				}
			});
		}

}




