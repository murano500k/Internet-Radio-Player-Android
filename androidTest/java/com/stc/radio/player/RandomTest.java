package com.stc.radio.player;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.stc.radio.player.model.MusicProvider;
import com.stc.radio.player.model.MusicProviderSource;
import com.stc.radio.player.model.RemoteJSONSource;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.stc.radio.player.StationTest.instrumentation;
import static com.stc.radio.player.StationTest.provider;
import static com.stc.radio.player.utils.MediaIDHelper.MEDIA_ID_ROOT;

/**
 * Created by artem on 12/1/16.
 */

public class RandomTest extends TestCase{


    public static final boolean DEBUG = true;
    private static int i;

    public String ASSERT_MESSAGE_PREFIX;

    private static final String TAG = "RandomTest";

    Instrumentation inst;

    public final int TEST_VIDEO = 0;
    public final int TEST_AUDIO = 1;
    public final int TEST_IMAGE = 2;
    public final int TEST_URL = 3;
    public final int TEST_APP = 4;


    public final int TESTS_COUNT = 5;
    BaseTest randomTest;
    public int random = -1;

    static TestSuite testSuite;
    private static boolean flag;
    static List<String >list;

    public void setAssertTestPrefix(String test_type) {
        ASSERT_MESSAGE_PREFIX = test_type + " ";
    }
@Test
    public void testTuite() {

        testSuite= new TestSuite(RandomTest.class.getName());
        list=new ArrayList<>();
        instrumentation= InstrumentationRegistry.getInstrumentation();

        provider= new MusicProvider(new RemoteJSONSource());

        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                provider.retrieveMediaAsync(new MusicProvider.Callback() {
                    @Override
                    public void onMusicCatalogReady(boolean success) {
                        Log.d(TAG, "onMusicCatalogReady: ");
                        instrumentation.waitForIdle(new Runnable() {
                            @Override
                            public void run() {
                                List<MediaBrowserCompat.MediaItem> genres = provider.getChildren(MEDIA_ID_ROOT, instrumentation.getTargetContext().getResources());
                                assertNotNull(genres);
                                for (String g : provider.getGenres()) {
                                    for (MediaMetadataCompat item : provider.getMusicsByGenre(g)) {
                                        String url = item.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                                        Log.d(TAG, "testStations: " + url);
                                        list.add(url);
                                    }
                                }
                                Log.d(TAG, "flag = true;");
                                flag = true;

                            }

                        });
                    }
                });


            };
        });
        while(!flag){

            SystemClock.sleep(4000);
        }
        assertNotNull(list);
        assertTrue(!list.isEmpty());
        i=0;
        for(String s: list){
            testSuite.addTestSuite(MediaTest.class);
        }
        instrumentation.waitForIdle(new Runnable() {
            @Override
            public void run() {
                testSuite.run(new TestResult());

            }
        });
    }
    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    public static String getUrl() {
        Log.d(TAG, i+" getUrl: "+list.get(i));
        return list.get(i++);
    }
}