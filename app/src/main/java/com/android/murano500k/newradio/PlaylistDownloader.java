package com.android.murano500k.newradio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.murano500k.newradio.ui.ActivityRxTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.android.murano500k.newradio.UrlManager.INDEX_CUSTOM;


public class PlaylistDownloader {
	private Context context;
	public PlaylistDownloader(Context applicationContext) {
		context=applicationContext;
	}

	public void downloadPlaylist(String urlStr, int index){
		Log.d("PlaylistDownloader","downloadPlaylist");
		Log.d("PlaylistDownloader","index="+index+" str="+urlStr);

		Observable.just(urlStr)
				.observeOn(Schedulers.newThread())
				.subscribeOn(AndroidSchedulers.mainThread())
				.subscribe(s -> {
					String res="";
					String line;
					try {
						InputStream inputStream;
						if(index==INDEX_CUSTOM) {
							Uri uri = Uri.parse(urlStr);
							inputStream=context.getContentResolver().openInputStream(uri);
						} else {
							URL url = new URL(urlStr);
							inputStream=url.openStream();
						}
						BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
						while ((line = in.readLine()) != null) {
							res+=line;
						}
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Log.w("download result",res);

					Intent intent = new Intent(context, ActivityRxTest.class);
					intent.setAction(ActivityRxTest.INTENT_UPDATE_STATIONS);
					intent.putExtra(ActivityRxTest.EXTRA_UPDATE_STATIONS_RESULT_STRING, res);
					intent.putExtra(ActivityRxTest.EXTRA_UPDATE_STATIONS_INDEX, index);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				});
	}

}
