package com.stc.radio.player.model;

import android.support.v4.media.MediaMetadataCompat;

import com.activeandroid.ActiveAndroid;
import com.stc.radio.player.contentmodel.ParsedPlaylistItem;
import com.stc.radio.player.contentmodel.Retro;
import com.stc.radio.player.contentmodel.StationsManager;
import com.stc.radio.player.db.DBMediaItem;
import com.stc.radio.player.utils.SettingsProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.DI;

/**
 * Created by artem on 12/1/16.
 */
public class AudioAddictSource extends BaseRemoteSource {
    public AudioAddictSource(String pls) {
        super(pls);
    }

    @Override
    public ArrayList<MediaMetadataCompat> loadStations() {
        Retro.updateToken();
        SettingsProvider.getToken();
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        Response<List<ParsedPlaylistItem>> response = null;
        Call<List<ParsedPlaylistItem>> loadSizeCall;
        if (name.equals(DI) || name.equals("di.fm")) {
            loadSizeCall = Retro.getStationsCall("di");
        } else loadSizeCall = Retro.getStationsCall(name);
        try {
            response = loadSizeCall.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response != null && response.isSuccessful() && !response.body().isEmpty()) {
            Timber.w("track 0=%s", response.body().get(0).getKey());
            for (int i = 0; i < response.body().size(); i++) {
                tracks.add(buildFromResponce(response.body().get(i), name));
            }
        }
        if (tracks.size() > 0) {
            try {
                ActiveAndroid.beginTransaction();

                for (MediaMetadataCompat metadata : tracks) {
                    DBMediaItem dbMediaItem = new DBMediaItem(metadata);
                    dbMediaItem.save();
                }
                ActiveAndroid.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ActiveAndroid.endTransaction();
            }
            return tracks;
        } else return null;
    }

    public MediaMetadataCompat buildFromResponce(ParsedPlaylistItem item, String pls){
        String source = StationsManager.getUrl(pls, item.getKey());
        String iconUrl = StationsManager.getArtUrl(item);
        String title =pls+" - "+item.getName();
        //Timber.w("Parsed: title=%s url=%s",title,source);
        return createMetadata(source, title, iconUrl);
    }

}
