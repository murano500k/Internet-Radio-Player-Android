/*
package com.stc.radio.player;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.model.RemoteJSONSource;
import com.stc.radio.player.playback.MyLocalPlayback;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;

import static android.R.attr.resource;
import static com.stc.radio.player.model.MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE;
import static com.stc.radio.player.ui.DialogShower.TAG;
import static junit.framework.Assert.assertTrue;

*/
/**
 * Created by artem on 11/30/16.
 *//*

//@RunWith(AndroidJUnit4.class)
public class StationTest {
    private static final long WAIT_TIME = 5;
  //  CountingIdlingResource resource=new CountingIdlingResource("test1");
    @Test
    public void testStations(){
       // Instrumentation instrumentation= InstrumentationRegistry.getInstrumentation();

        MusicProvider provider= new MusicProvider(new RemoteJSONSource());
        MyLocalPlayback playback=new MyLocalPlayback(instrumentation.getTargetContext(),provider);
        //resource.increment();
        while (provider.getGenres()==null){
            SystemClock.sleep(1000);
        }
        resource.increment();
        for(String genre : provider.getGenres()){
            Iterator<MediaMetadataCompat> metadataCompatList = provider.getMusicsByGenre(genre).iterator();
            while (metadataCompatList.hasNext()){
                MediaMetadataCompat metadata = metadataCompatList.next();
                playback.setCurrentMediaId(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) );
                playback.start();
                SystemClock.sleep(WAIT_TIME*1000);
                assertTrue(playback.isPlaying());
                Log.d(TAG, "testStations: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                Log.d(TAG, "testStations: "+metadata.getString(CUSTOM_METADATA_TRACK_SOURCE));
                Log.d(TAG, "testStations: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                Log.d(TAG, "testStations: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            }



        }
    }
}
*/
