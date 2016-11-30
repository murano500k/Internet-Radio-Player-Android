package com.stc.radio.player;

/**
 * Created by artem on 12/1/16.
 */


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by artem on 11/17/16.
 */
@RunWith(AndroidJUnit4.class)
public class MediaTest extends TestCase {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(
            TestActivity.class);
    private MediaPlayer mMediaPlayer;
    private int WAIT_FOR_COMMAND_TO_COMPLETE = 60000;  //1 min max.
    private boolean mPrepareReset = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();
    private final Object mPrepareDone = new Object();
    private final Object mOnCompletion = new Object();
    private final long PAUSE_WAIT_TIME = 2000;
    private final long WAIT_TIME = 2000;
    private final int SEEK_TIME = 10000;
    private boolean mInitialized = false;

    private boolean mOnPrepareSuccess = false;
    public boolean mOnCompleteSuccess = false;
    public boolean mPlaybackError = false;
    public int mMediaInfoUnknownCount = 0;
    public int mMediaInfoVideoTrackLaggingCount = 0;
    public int mMediaInfoBadInterleavingCount = 0;
    public int mMediaInfoNotSeekableCount = 0;
    public int mMediaInfoMetdataUpdateCount = 0;
    String url;
    static Instrumentation inst;
    public String packageName;

    public static String BASE_PATH_VIDEO = "/sdcard/test/rvideo/";
    public static String BASE_PATH_AUDIO = "/sdcard/test/raudio/";
    public static String BASE_PATH_IMAGE = "/sdcard/test/rimage/";
    public static String BASE_PATH_URL = "/sdcard/test/urls.txt";
    public static String BASE_PATH_APP = "/sdcard/test/rapk/";
    public String TAG = "BaseTest";
    static String ASSERT_MESSAGE_PREFIX;
    TestActivity activity;
    static String fileName;


    public void initBase(Instrumentation inst,  String msgPrefix){
        this.ASSERT_MESSAGE_PREFIX=msgPrefix;
        this.inst = inst;
    }

    public MediaTest() {
        TAG = "MediaTest";
        this.url=RandomTest.getUrl();
        initBase(InstrumentationRegistry.getInstrumentation(), url);
    }

    @Test
    public boolean testMediaSample(final String url ) {
        activity = launchMainActivity();
        int duration = 0;
        int waittime = 0;
        mOnCompleteSuccess = false;
        mMediaInfoUnknownCount = 0;
        mMediaInfoVideoTrackLaggingCount = 0;
        mMediaInfoBadInterleavingCount = 0;
        mMediaInfoNotSeekableCount = 0;
        mMediaInfoMetdataUpdateCount = 0;
        mPlaybackError = false;
        initializeMessageLooper();
        synchronized (mLock) {
            try {
                mLock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
            } catch(Exception e) {
                fail(ASSERT_MESSAGE_PREFIX+" looper was interrupted.");
                return false;
            }
        }
        try {
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            Log.v(TAG, "playMediaSample: sample file name " + url);
            mMediaPlayer.setDataSource(url);
            setTitle(activity, ASSERT_MESSAGE_PREFIX);
            mMediaPlayer.setDisplay(activity.getSurfaceView().getHolder());
            mMediaPlayer.prepare();
            duration = mMediaPlayer.getDuration();
            Log.v(TAG, "duration of media " + duration);
            mMediaPlayer.start();
            SystemClock.sleep(3000);
            //mMediaPlayer.stop();
            assertTrue(ASSERT_MESSAGE_PREFIX+"wait timed-out without onCompletion notification",mOnCompleteSuccess);
            terminateMessageLooper();
            if(activity!=null)activity.finish();

        } catch (Exception e) {
            e.printStackTrace();
            fail(ASSERT_MESSAGE_PREFIX + e.getMessage());
        }


        return mOnCompleteSuccess;
    }
    public void setTitle(final Activity act, final String fileName) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int lastSlash=fileName.lastIndexOf("/")+1;
                String title=ASSERT_MESSAGE_PREFIX;
                //if(lastSlash>0)	title+=fileName.substring(lastSlash);
                //else title+=fileName;
                act.setTitle(title);
            }
        });
    }
    private void initializeMessageLooper() {
        Log.v(TAG, "start looper");
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Log.v(TAG, "start loopRun");
                mLooper = Looper.myLooper();
                mMediaPlayer = new MediaPlayer();
                synchronized (mLock) {
                    mInitialized = true;
                    mLock.notify();
                }
                Looper.loop();
                Log.v(TAG, "initializeMessageLooper: quit.");
            }
        }.start();
    }

    public TestActivity launchMainActivity(){
        Intent intent = new Intent();
        intent.setClass(inst.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
        //return activity;
        return  (TestActivity) inst.startActivitySync(intent);
    }

    private void terminateMessageLooper() {
        mLooper.quit();
        mMediaPlayer.release();
    }

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            synchronized (mPrepareDone) {
                if(mPrepareReset) {
                    Log.v(TAG, "call Reset");
                    mMediaPlayer.reset();
                }
                Log.v(TAG, "notify the prepare callback");
                mOnPrepareSuccess = true;
            }
        }
    };
    MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            synchronized (mOnCompletion) {
                Log.v(TAG, "notify the completion callback");
                mOnCompletion.notify();
                mOnCompleteSuccess = true;
            }
        }
    };

    MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            Log.v(TAG, "playback error");
            mPlaybackError = true;
            mp.reset();
            fail(ASSERT_MESSAGE_PREFIX+"playback error");
            return true;
        }
    };

    MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_INFO_UNKNOWN:
                    mMediaInfoUnknownCount++;
                    break;
                case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                    mMediaInfoVideoTrackLaggingCount++;
                    fail(ASSERT_MESSAGE_PREFIX+"MEDIA_INFO_VIDEO_TRACK_LAGGING");

                    break;
                case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                    mMediaInfoBadInterleavingCount++;
                    fail(ASSERT_MESSAGE_PREFIX+"MEDIA_INFO_BAD_INTERLEAVING");

                    break;
                case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                    mMediaInfoNotSeekableCount++;
                    fail(ASSERT_MESSAGE_PREFIX+"MEDIA_INFO_NOT_SEEKABLE");
                    break;
                case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    mMediaInfoMetdataUpdateCount++;
                    break;
            }
            return true;
        }
    };

    @Override
    public int countTestCases() {
        return 1;
    }

    @Override
    public void run(TestResult testResult) {

    }
}
