package com.stc.radio.player.model;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;

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

    public ArrayList<MediaMetadataCompat> getStations() {
        ArrayList<MediaMetadataCompat> list = checkDB();
        //ArrayList<MediaMetadataCompat> list = null;
        if (list == null || list.isEmpty()) {
            list = loadStations();
            if (list == null || list.isEmpty())return null;
            else {

                try {
                ActiveAndroid.beginTransaction();
                    for (MediaMetadataCompat metadata : list) {
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


    protected ArrayList<MediaMetadataCompat> checkDB() {


	    boolean exists=false;

        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        From from = new Select().from(DBMediaItem.class).orderBy("PlayedTimes").orderBy("Favorite");
        if (from.exists()) {
            List<DBMediaItem> listDBAll = from.execute();
            if (listDBAll != null && !listDBAll.isEmpty() && listDBAll.size() > 0) {
                //listDBAll=RatingHelper.sortByPlayedTimes(listDBAll);
                for (DBMediaItem dbMediaItem : listDBAll) {
                    //if(dbMediaItem.isFavorite())Log.w("DBITEM",dbMediaItem.getTitle()+" "+dbMediaItem.isFavorite());
                    MediaMetadataCompat metadata = createMetadata(dbMediaItem);
                    if (metadata == null) {
                        Timber.e("ERROR metadata  is null");
                    } else if (metadata
                            .getString(MediaMetadataCompat.METADATA_KEY_TITLE)
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

    protected abstract ArrayList<MediaMetadataCompat> loadStations();

    public static MediaMetadataCompat createMetadata(
            String id, String source, String title, String iconUrl, RatingCompat rating){
        //Timber.w("Parsed: title=%s url=%s",title,source);
        assertNotNull(id);
        assertNotNull(source);
        assertNotNull(title);
        assertNotNull(rating);
        return new MediaMetadataCompat.Builder()
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING, rating)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
                .build();
    }




    public static MediaMetadataCompat createMetadata(String source, String title, String iconUrl){
        Integer idNum = source.hashCode();
        String id=idNum.toString();
        RatingCompat ratingCompat= RatingCompat.newHeartRating(false);
        return createMetadata(id, source, title, iconUrl, ratingCompat);
    }
    public static MediaMetadataCompat createMetadata(DBMediaItem item) {
        String iconUrl=item.getIconUri();
        String title=item.getTitle();
        String id=item.getMediaId();
        String source=item.getSource();
        int playedTimes = item.getPlayedTimes();
        boolean isFav=item.isFavorite();

        //float userRatingPercentage = RatingHelper.getUserRatingPercentageForItem(isFav, playedTimes);
        RatingCompat ratingCompat=RatingCompat.newHeartRating(isFav);
        //Timber.w("item:%s,fav: %b, played:%d", item.getTitle(), isFav, playedTimes);
        return createMetadata(id, source, title, iconUrl, ratingCompat);
    }

}



