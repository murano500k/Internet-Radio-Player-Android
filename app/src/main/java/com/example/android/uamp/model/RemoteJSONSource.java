/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.model;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.example.android.uamp.utils.LogHelper;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;
import com.stc.radio.player.contentmodel.Retro;
import com.stc.radio.player.contentmodel.StationsManager;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.SettingsProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static com.google.android.gms.analytics.internal.zzy.D;
import static com.google.android.gms.analytics.internal.zzy.e;
import static com.google.android.gms.analytics.internal.zzy.p;
import static com.google.android.gms.cast.internal.zzl.pk;
import static com.google.android.gms.internal.zzng.fa;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.DI;
import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.SOMA;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class RemoteJSONSource implements MusicProviderSource {

	private static final String TAG = LogHelper.makeLogTag(RemoteJSONSource.class);

	private static final String JSON_MUSIC = "music";
	private static final String JSON_TITLE = "title";
	private static final String JSON_ALBUM = "album";
	private static final String JSON_ARTIST = "artist";
	private static final String JSON_GENRE = "genre";
	private static final String JSON_SOURCE = "source";
	private static final String JSON_IMAGE = "image";
	private static final String JSON_TRACK_NUMBER = "trackNumber";
	private static final String JSON_TOTAL_TRACK_COUNT = "totalTrackCount";
	private static final String JSON_DURATION = "duration";

	@Override
	public Iterator<MediaMetadataCompat> iterator() {
		Retro.updateToken();
		SettingsProvider.getToken();
		ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
		try {
			From from = new Select().from(DBMediaItem.class);
			if(from.exists()){
				List<DBMediaItem> listDB=from.execute();
				if(listDB!=null && !listDB.isEmpty() && listDB.size()>100){
					for(DBMediaItem dbMediaItem: listDB) tracks.add(dbMediaItem.createMetadata());
				}
			}else {
				String[] playlists = {
						StationsManager.PLAYLISTS.DI,
						StationsManager.PLAYLISTS.CLASSIC,
						StationsManager.PLAYLISTS.JAZZ,
						StationsManager.PLAYLISTS.ROCK,
						StationsManager.PLAYLISTS.RADIOTUNES,
						StationsManager.PLAYLISTS.SOMA
				};

				for (String pls : playlists) {
					Response<List<ParsedPlaylistItem>> response = null;
					Call<List<ParsedPlaylistItem>> loadSizeCall;
					if (pls.equals(SOMA)) {

						int i = 0;
						for (String s1 : StationsManager.Soma.somaStations) {
							tracks.add(buildFromResponce(s1, pls, i, StationsManager.Soma.somaStations.length, 0));
							i++;
						}
					}
					if (pls.equals(DI) || pls.equals("di.fm")) {
						loadSizeCall = Retro.getStationsCall("di");
					} else loadSizeCall = Retro.getStationsCall(pls);
					try {
						response = loadSizeCall.execute();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					if (response != null && response.isSuccessful() && !response.body().isEmpty()) {
						//Timber.w("track 0=%s", response.body().get(0).getKey());

						for (int i = 0; i < response.body().size(); i++) {
							tracks.add(buildFromResponce(response.body().get(i), pls, i, response.body().size(), 0));
						}
					}
				}

				if(tracks.size()>100) {
					try{
						ActiveAndroid.beginTransaction();
						for(MediaMetadataCompat metadata : tracks){
							DBMediaItem dbMediaItem = new DBMediaItem(metadata);
							dbMediaItem.save();
						}
						ActiveAndroid.setTransactionSuccessful();
					}catch (Exception e){
						e.printStackTrace();
					} finally {
						ActiveAndroid.endTransaction();
					}
				}
			}
		} catch (Exception e) {
			LogHelper.e(TAG, e, "Could not retrieve music list");
			throw new RuntimeException("Could not retrieve music list", e);
		}
		Timber.w("list size=%d", tracks.size());

		return tracks.iterator();

	}
	private MediaMetadataCompat buildFromResponce(String station,  String pls, int i, int totalTrackCount, long favorite) {

		String title = station;
		String source = StationsManager.getUrl(pls, station);
		String iconUrl = StationsManager.getArtUrl(station);

		int trackNumber = i;
		int duration = -1; // ms

		String id = String.valueOf(source.hashCode());
		Character character = station.charAt(0);
		String name = station.replaceFirst(character.toString(), character.toString().toUpperCase());
		Timber.w("Parsed: iconUrl=%s url=%s",iconUrl,source);
		return new MediaMetadataCompat.Builder()
				.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
				.putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
				.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
				.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
				.putString(MediaMetadataCompat.METADATA_KEY_GENRE, pls)
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, name)
				.build();

	}

	private MediaMetadataCompat buildFromResponce(ParsedPlaylistItem item, String pls, int i, int totalTrackCount, long favorite) {

		String title =item.getName();
		String source = StationsManager.getUrl(pls, item.getKey());
		String iconUrl = StationsManager.getArtUrl(item);

		int trackNumber = i;
		int duration = -1; // ms
		String id = String.valueOf(source.hashCode());
		Timber.w("Parsed: iconUrl=%s url=%s",iconUrl,source);

		return new MediaMetadataCompat.Builder()
				.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
				.putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
				.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
				.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
				.putString(MediaMetadataCompat.METADATA_KEY_GENRE, pls)
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
				.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, title)
				.build();
	}
}



