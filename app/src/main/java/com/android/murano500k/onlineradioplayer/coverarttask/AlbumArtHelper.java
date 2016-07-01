package com.android.murano500k.onlineradioplayer.coverarttask;

import android.util.Log;

import java.util.concurrent.ExecutionException;

/**
 * Created by artem on 6/23/16.
 */
public class AlbumArtHelper{

    private static final String TAG = "AlbumArtHelper";

    public static void getArt(String artist, String song) {
        AsyncGetReleaseGroupIDFromMusicBrainzTask asyncGetReleaseGroupIDFromMusicBrainzTask = new AsyncGetReleaseGroupIDFromMusicBrainzTask();
        asyncGetReleaseGroupIDFromMusicBrainzTask.execute("The fat of the land", "The Prodigy");
        String releaseGroupID = "";
        try {
            releaseGroupID = asyncGetReleaseGroupIDFromMusicBrainzTask.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(TAG, "releaseGroupID: "+releaseGroupID);

        if (releaseGroupID != null) {

            AsyncGetCoverArtFromCoverArtArchiveTask asyncGetCoverArtFromCoverArtArchiveTask = new AsyncGetCoverArtFromCoverArtArchiveTask();
            asyncGetCoverArtFromCoverArtArchiveTask.execute(releaseGroupID);
            try {
                Log.d(TAG,"asyncGetCoverArtFromCoverArtArchiveTask get : "+ asyncGetCoverArtFromCoverArtArchiveTask.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }


        } else {
            Log.d(TAG, "No release ID");
        }
    }
}
