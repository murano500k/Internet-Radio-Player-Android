package com.stc.radio.player;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.model.MusicProviderSource;
import com.stc.radio.player.model.RemoteJSONSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.stc.radio.player.ui.DialogShower.TAG;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by artem on 11/30/16.
 */
@RunWith(AndroidJUnit4.class)
public class StationTest {
    private static final long WAIT_TIME = 5;
    static Instrumentation instrumentation;
    static TestActivity a;
    static MusicProvider provider;
    private static boolean flag=false;



    @Test
    public void testStations(){
    instrumentation= InstrumentationRegistry.getInstrumentation();
    a=(TestActivity) instrumentation.startActivitySync(new Intent(
                instrumentation.getTargetContext(), TestActivity.class));
        provider= new MusicProvider(new RemoteJSONSource());


        //MyLocalPlayback playback = new MyLocalPlayback(instrumentation.getTargetContext(), provider);


        instrumentation.waitForIdle(new Runnable() {
            @Override
            public void run() {

                provider.retrieveMediaAsync(callback);
                Log.d(TAG, "testStations: ");

            };
        });
        while(!flag){

            SystemClock.sleep(4000);
        }
    }
       public static MusicProvider.Callback callback= new MusicProvider.Callback() {
           @Override
           public void onMusicCatalogReady(boolean success) {
               Log.d(TAG, "onMusicCatalogReady: ");
               instrumentation.waitForIdle(new Runnable() {
                   @Override
                   public void run() {
                       List<MediaBrowserCompat.MediaItem> genres = provider.getChildren(MEDIA_ID_ROOT, a.getResources());
                       assertNotNull(genres);
                       for (String g : provider.getGenres()) {
                           for (MediaMetadataCompat item : provider.getMusicsByGenre(g)) {
                               String url = item.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                               Log.d(TAG, "testStations: " + url);

                                while(true){
                                       SystemClock.sleep(1000);
                                   }
                           }
                       }
                       flag = false;

                   }

               });
           }
       };




}
