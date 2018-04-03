package com.stc.radio.player.source;


import android.media.MediaMetadata;
import android.media.Rating;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.stc.radio.player.db.DBMediaItem;
import com.stc.radio.player.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;


public abstract class BaseRemoteSource {

    private static final String TAG = LogHelper.makeLogTag(RemoteSource.class);
    public final String name;

    public String getName() {
        return name;
    }

    public BaseRemoteSource(String name) {
        this.name = name;
    }

    public ArrayList<MediaMetadata> getStations() {
        ArrayList<MediaMetadata> list = checkDB();
        //ArrayList<MediaMetadata> list = null;
        if (list == null || list.isEmpty()) {
            list = loadStations();
            if (list == null || list.isEmpty())return null;
            else {

                try {
                ActiveAndroid.beginTransaction();
                    for (MediaMetadata metadata : list) {
                    DBMediaItem dbMediaItem = new DBMediaItem(metadata);
                    dbMediaItem.save();
                }
                ActiveAndroid.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    ActiveAndroid.endTransaction();
                }
            }
        }
        return list;
    }


    protected ArrayList<MediaMetadata> checkDB() {


	    boolean exists=false;

        ArrayList<MediaMetadata> tracks = new ArrayList<>();
        From from = new Select().from(DBMediaItem.class).orderBy("PlayedTimes").orderBy("Favorite");
        if (from.exists()) {
            List<DBMediaItem> listDBAll = from.execute();
            if (listDBAll != null && !listDBAll.isEmpty() && listDBAll.size() > 0) {
                //listDBAll=RatingHelper.sortByPlayedTimes(listDBAll);
                for (DBMediaItem dbMediaItem : listDBAll) {
                    //if(dbMediaItem.isFavorite())Log.w("DBITEM",dbMediaItem.getTitle()+" "+dbMediaItem.isFavorite());
                    MediaMetadata metadata = createMetadata(dbMediaItem);
                    if (metadata == null) {
                        Timber.e("ERROR metadata  is null");
                    } else if (metadata
                            .getString(MediaMetadata.METADATA_KEY_TITLE)
                            .contains(this.getName())) {
                        tracks.add(metadata);
                        exists=true;
                    }
                }
            }
        }
            if (exists && !tracks.isEmpty() && tracks.size() > 0) {
                Timber.w("list size=%d", tracks.size());

                return tracks;
            }
        Timber.w("No items in db");
        return null;
    }

    protected abstract ArrayList<MediaMetadata> loadStations();

    public static MediaMetadata createMetadata(
            String id, String source, String title, String iconUrl, Rating rating){
        //Timber.w("Parsed: title=%s url=%s",title,source);
        assertNotNull(id);
        assertNotNull(source);
        assertNotNull(title);
        assertNotNull(rating);
        return new MediaMetadata.Builder()
                .putRating(MediaMetadata.METADATA_KEY_USER_RATING, rating)
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadata.METADATA_KEY_ART_URI, iconUrl)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
                .build();
    }




    public static MediaMetadata createMetadata(String source, String title, String iconUrl){
        Integer idNum = source.hashCode();
        String id=idNum.toString();
        Rating rating= Rating.newHeartRating(false);
        return createMetadata(id, source, title, iconUrl, rating);
    }
    public static MediaMetadata createMetadata(DBMediaItem item) {
        String iconUrl=item.getIconUri();
        String title=item.getTitle();
        String id=item.getMediaId();
        String source=item.getSource();
        int playedTimes = item.getPlayedTimes();
        boolean isFav=item.isFavorite();

        //float userRatingPercentage = RatingHelper.getUserRatingPercentageForItem(isFav, playedTimes);
        Rating rating=Rating.newHeartRating(isFav);
        //Timber.w("item:%s,fav: %b, played:%d", item.getTitle(), isFav, playedTimes);
        return createMetadata(id, source, title, iconUrl, rating);
    }

}



