package com.stc.radio.player.source;

import android.support.v4.media.MediaMetadataCompat;

import com.stc.radio.player.model.StationsManager;

import java.util.ArrayList;

import static com.stc.radio.player.model.StationsManager.PLAYLISTS.SOMA;

/**
 * Created by artem on 12/1/16.
 */

public class SomaRemoteSource extends BaseRemoteSource {
    public SomaRemoteSource(String pls) {
        super(pls);
    }

    @Override
    public ArrayList<MediaMetadataCompat> loadStations() {
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<MediaMetadataCompat>();
            if (name.equals(SOMA)) {
                for (String s1 : StationsManager.Soma.somaStations) {
                    tracks.add(buildFromResponce(s1, name));
                }
            }
        return tracks;
    }

    public MediaMetadataCompat buildFromResponce(String station,  String pls){
        String source = StationsManager.getUrl(pls, station);
        String iconUrl = StationsManager.getArtUrl(station);
        Character character = station.charAt(0);
        String name = station.replaceFirst(character.toString(), character.toString().toUpperCase());
        String title = pls+" - "+station;
        //Timber.w("Parsed: source=%s", source);
        //Timber.w("Parsed: title=%s", title);
        //Timber.w("Parsed: iconUrl=%s", iconUrl);
        //Timber.w("");
        return createMetadata(source, title, iconUrl);
    }
}
