package com.android.murano500k.onlineradioplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.ArrayList;
import java.util.List;


        
 

public class RadioPlayerService extends Service implements PlayerCallback {
    public static final String NOTIFICATION_INTENT_PREV = "Radio.INTENT_PREV";

    public static final String NOTIFICATION_INTENT_NEXT = "Radio.INTENT_NEXT";

    public static final String NOTIFICATION_INTENT_PLAY_PAUSE = "Radio.INTENT_PLAYPAUSE";

    public static final String WIDGET_INTENT_PREV = "Radio.WIDGET_INTENT_PREV";

    public static final String WIDGET_INTENT_NEXT = "Radio.WIDGET_INTENT_NEXT";

    public static final String WIDGET_INTENT_PLAY_PAUSE = "Radio.WIDGET_INTENT_PLAYPAUSE";

    public static final String NOTIFICATION_INTENT_CANCEL = "Radio.INTENT_CANCEL";

    public static final String NOTIFICATION_INTENT_OPEN_PLAYER = "Radio.INTENT_OPENPLAYER";
    public static final String CONNECT_INTENT_CONNECTED = "Radio.CONNECT_INTENT_CONNECTED";
    public static final String CONNECT_INTENT_DISCONNECTED = "Radio.CONNECT_INTENT_DISCONNECTED";
    private static final String SHUFFLE_KEY = "Radio.SHUFFLE_KEY";
    private static final String NO_URL = "EMPTY_NO_URL";

    public static final String LEFT_SECONDS = "com.android.murano500k.onlineradioplayer.LEFT_MINUTES";
    public static final String SLEEP_INTENT_SET_TIMER = "Radio.SLEEP_INTENT_SET_TIMER";
    public static final String SLEEP_INTENT_CANCEL_TIMER = "Radio.SLEEP_INTENT_CANCEL_TIMER";

    private String stationName = "";
    private String singerName = "";
    private String next = "";
    private String prev = "";
    private String songName = "";
    private int smallImage = R.drawable.default_art;
    private Bitmap artImage;

    public static final int NOTIFICATION_ID = 001;
    private static boolean isLogging = false;

    private final int AUDIO_BUFFER_CAPACITY_MS = 800;
    private final int AUDIO_DECODE_CAPACITY_MS = 400;
  private final String SUFFIX_PLS = ".pls";
    private final String SUFFIX_RAM = ".ram";
    private final String SUFFIX_WAX = ".wax";
    private final String TAG= "RadioPlayerService";
    private MainActivity activity;
    public static final String SLEEP_TIMER_CANCEL = "Radio.SLEEP_TIMER_CANCEL";
    public static final String SLEEP_TIMER_FINISH = "Radio.SLEEP_TIMER_FINISH";
    public static final String SLEEP_TIMER_START = "Radio.SLEEP_TIMER_START";
    public static final String SLEEP_TIMER_UPDATE = "Radio.SLEEP_TIMER_UPDATE";
    private SleepTimerTask sleepTimerTask;


    public enum State {
        IDLE,
        PLAYING,
        STOPPED,
    }

    List<RadioListener> mListenerList;

    private State mRadioState;

    private String mRadioUrl;

    public static final String ACTION_MEDIAPLAYER_STOP = "co.mobiwise.library.ACTION_STOP_MEDIAPLAYER";


    private MultiPlayer mRadioPlayer;


    private TelephonyManager mTelephonyManager;

    private boolean isSwitching;


    private boolean isClosedFromNotification = false;


    private boolean isInterrupted;

    private boolean mLock;

    private NotificationManager mNotificationManager;

    public final IBinder mLocalBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;

    }

    public boolean isClosedFromNotification() {
        Log.d(TAG, "isClosedFromNotification: "+ isClosedFromNotification);
        return isClosedFromNotification;
    }

    public void setClosedFromNotification(boolean closedFromNotification) {
        isClosedFromNotification = closedFromNotification;
    }

    public class LocalBinder extends Binder {
        public RadioPlayerService getService() {
            return RadioPlayerService.this;
        }
    }

    public void setActivity(MainActivity activity){
        this.activity = activity;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG,"onStartCommand " + action);
        Log.d(TAG, "NOTIFICATION_INTENT " + action);
        if (action.equals(SLEEP_INTENT_SET_TIMER)) {
            Log.d(TAG, "serv SLEEP_INTENT_SET_TIMER");

            int secs = intent.getIntExtra(LEFT_SECONDS, -254);
            if(secs==-254)             Log.d(TAG, "secs ERROR");

            Log.d(TAG, "secs" + secs);
            setSleepTimer(secs);
        }else if (action.equals(SLEEP_INTENT_CANCEL_TIMER)) {
            Log.d(TAG, "serv SLEEP_INTENT_CANCEL_TIMER");
            cancelSleepTimer();
        }else if(action.equals(CONNECT_INTENT_CONNECTED)) {
            Log.d(TAG, "serv CONNECT_INTENT_CONNECTED");
            if (isInterrupted && !isPlaying()) {
                Log.d(TAG, "Player was interrupted. Now internet available. Resuming playback");
                isInterrupted = false;
                play(mRadioUrl);
            } else {
                Log.d(TAG, "Player was not interrupted. Don't resume playback");
            }
        } else if (action.equals(NOTIFICATION_INTENT_CANCEL)) {
            Log.d(TAG,"serv NOTIFICATION_INTENT_CANCEL");
            isClosedFromNotification=true;
            if(isPlaying())stop();

            if(mNotificationManager != null) {
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
        } else if (action.equals(NOTIFICATION_INTENT_PLAY_PAUSE)) {
            Log.d(TAG, "NOTIFICATION_INTENT_PLAY_PAUSE");
            isClosedFromNotification=false;
            if(isPlaying())stop();
            else notifyCurrentSongShouldPlay();
        } else if (action.equals(NOTIFICATION_INTENT_PREV)) {
            Log.d(TAG, "NOTIFICATION_INTENT_PREV");
            isClosedFromNotification=false;
            if(isPlaying())stop();
            notifyPrevSongShouldPlay();
        } else if (action.equals(NOTIFICATION_INTENT_NEXT)) {
            Log.d(TAG, "NOTIFICATION_INTENT_NEXT");
            isClosedFromNotification=false;
            if(isPlaying())stop();
            notifyNextSongShouldPlay();
        }
    return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();


        mListenerList = new ArrayList<>();

        mRadioState = State.IDLE;
        isSwitching = false;
        isInterrupted = false;
        mLock = false;
        getPlayer();

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


     
     
     
     

    public void play(String mRadioUrl) {
        if(mRadioUrl==null || mRadioUrl.equals(NO_URL)) mRadioUrl=this.mRadioUrl;
        sendBroadcast(new Intent(ACTION_MEDIAPLAYER_STOP));

        notifyRadioLoading();

        if (checkSuffix(mRadioUrl))
            decodeStremLink(mRadioUrl);
        else {
            this.mRadioUrl = mRadioUrl;
            isSwitching = false;

            if (isPlaying()) {
                log("Switching Radio");
                isSwitching = true;
                stop();
            } else if (!mLock) {
                log("Play requested.");
                mLock = true;
                getPlayer().playAsync(mRadioUrl);
            }
        }
    }

    public void stop() {
        if (!mLock && mRadioState != State.STOPPED) {
            log("Stop requested.");
            mLock = true;
            getPlayer().stop();
        }
    }

    /*public void buildNotification(){

    }
    public void updateNotification(String s1, String s2, int i1, int bi2){

    }
    public void updateNotification(String s1, String s2, int i1, Bitmap bi2){

    }*/

    @Override
    public void playerStarted() {
        mRadioState = State.PLAYING;
        buildNotification();
        mLock = false;
        notifyRadioStarted();

        log("Player started. tate : " + mRadioState);

        if (isInterrupted)
            isInterrupted = false;
        isClosedFromNotification = false;

    }

    public boolean isPlaying() {
        if (State.PLAYING == mRadioState)
            return true;
        return false;
    }

    @Override
    public void playerPCMFeedBuffer(boolean b, int i, int i1) {
        //Empty
    }

    @Override
    public void playerStopped(int i) {

        mRadioState = State.STOPPED;
        if (!isClosedFromNotification) {
            buildNotification();
            notifyRadioStopped(true);
        } else{
            notifyRadioStopped(false);
            isClosedFromNotification = false;
        }
        mLock = false;
        log("Player stopped. State : " + mRadioState);
        if (isSwitching)
            play(mRadioUrl);

    }

    @Override
    public void playerException(Throwable throwable) {
        mLock = false;
        mRadioPlayer = null;
        getPlayer();
        notifyErrorOccured();
        Log.d(TAG,"ERROR OCCURED.");
    }

    @Override
    public void playerMetadata(String s, String s2) {
        notifyMetaDataChanged(s, s2);
    }

    @Override
    public void playerAudioTrackCreated(AudioTrack audioTrack) {
        //Empty
    }

    public void registerListener(RadioListener mListener) {
        mListenerList.add(mListener);
    }

    public void unregisterListener(RadioListener mListener) {
        mListenerList.remove(mListener);
    }

    private void notifyRadioStarted() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioStarted();
        }
    }

    private void notifyRadioStopped(boolean updateNotification) {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onRadioStopped(updateNotification);
    }

    private void notifyMetaDataChanged(String s, String s2) {
        for (RadioListener mRadioListener : mListenerList)
            mRadioListener.onMetaDataReceived(s, s2);
    }

    private void notifyRadioLoading() {
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onRadioLoading();
        }
    }

    private void notifyErrorOccured(){
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onError();
        }
    }
    private void notifyCurrentSongShouldPlay(){
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onCurrentSongShouldPlay();
        }
    }
    private void notifyNextSongShouldPlay(){
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onNextSongShouldPlay();
        }
    }
    private void notifyPrevSongShouldPlay(){
        for (RadioListener mRadioListener : mListenerList) {
            mRadioListener.onPrevSongShouldPlay();
        }
    }

    private void notifySleepTimerStatusUpdate(String action, int seconds){
        for (RadioListener mRadioListener : mListenerList) {
            Log.d(TAG, "notifySleepTimerStatusUpdate");
            mRadioListener.onSleepTimerStatusUpdate(action, seconds);
        }
    }

    public MultiPlayer getPlayer() {
        try {

            java.net.URL.setURLStreamHandlerFactory(new java.net.URLStreamHandlerFactory() {

                public java.net.URLStreamHandler createURLStreamHandler(String protocol) {
                    Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
                    if ("icy".equals(protocol))
                        return new com.spoledge.aacdecoder.IcyURLStreamHandler();
                    return null;
                }
            });
        } catch (Throwable t) {
            Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
        }

        if (mRadioPlayer == null) {
            mRadioPlayer = new MultiPlayer(this, AUDIO_BUFFER_CAPACITY_MS, AUDIO_DECODE_CAPACITY_MS);
            mRadioPlayer.setResponseCodeCheckEnabled(false);
            mRadioPlayer.setPlayerCallback(this);
        }
        return mRadioPlayer;
    }
    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (isInterrupted)
                    play(mRadioUrl);
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if (isPlaying()) {
                    isInterrupted = true;
                    stop();
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    public boolean checkSuffix(String streamUrl) {
        if (streamUrl.contains(SUFFIX_PLS) ||
                streamUrl.contains(SUFFIX_RAM) ||
                streamUrl.contains(SUFFIX_WAX))
            return true;
        else
            return false;
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioPlayerService : " + log);
    }
    private void decodeStremLink(String streamLink) {
        new StreamLinkDecoder(streamLink) {
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                play(s);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void buildNotification() {

        Intent intentPlayPause = new Intent(NOTIFICATION_INTENT_PLAY_PAUSE);
        Intent intentPrev = new Intent(NOTIFICATION_INTENT_PREV);
        Intent intentNext = new Intent(NOTIFICATION_INTENT_NEXT);
        Intent intentOpenPlayer = new Intent(NOTIFICATION_INTENT_OPEN_PLAYER);
        intentOpenPlayer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent intentCancel = new Intent(NOTIFICATION_INTENT_CANCEL);
        /*
        intentPlayPause.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentPrev.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intentNext.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intentCancel.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
                */
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
        intentNext.addCategory(Intent.CATEGORY_DEFAULT);
        intentOpenPlayer.addCategory(Intent.CATEGORY_DEFAULT);
        intentCancel.addCategory(Intent.CATEGORY_DEFAULT);

         
         


        PendingIntent prevPending = PendingIntent.getService(getApplicationContext(), 0, intentPrev, 0);
        PendingIntent nextPending = PendingIntent.getService(getApplicationContext(),  0, intentNext, 0);
        PendingIntent playPausePending = PendingIntent.getService(getApplicationContext(), 0, intentPlayPause, 0);

        PendingIntent openPending = PendingIntent.getActivity(getApplicationContext(), 0, intentOpenPlayer, 0);
        PendingIntent cancelPending = PendingIntent.getService(getApplicationContext(), 0, intentCancel, 0);

        RemoteViews mNotificationTemplate = new RemoteViews(this.getPackageName(), R.layout.notification);
        Notification.Builder notificationBuilder = new Notification.Builder(this);

        if (artImage == null)
            artImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_checked);
        mNotificationTemplate.setTextViewText(R.id.notification_station_name, stationName);
        mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
        mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
        mNotificationTemplate.setImageViewResource(R.id.notification_play, isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
        mNotificationTemplate.setImageViewBitmap(R.id.widget_image, artImage);
         

        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_prev, prevPending);
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_next, nextPending);
        mNotificationTemplate.setOnClickPendingIntent(R.id.notification_play, playPausePending);


        Notification notification = notificationBuilder
                .setSmallIcon(smallImage)
                .setContentIntent(openPending)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContent(mNotificationTemplate)
                .setUsesChronometer(true)
                .build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

         

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            RemoteViews mExpandedView = new RemoteViews(this.getPackageName(), R.layout.notification_expanded);
            mExpandedView.setTextViewText(R.id.notification_station_name, stationName);
            mExpandedView.setTextViewText(R.id.notification_line_one, singerName);
            mExpandedView.setTextViewText(R.id.notification_line_two, songName);
            mExpandedView.setImageViewResource(R.id.notification_expanded_prev, R.drawable.ic_prev);
            mExpandedView.setImageViewResource(R.id.notification_expanded_next, R.drawable.ic_next);
            mExpandedView.setImageViewResource(R.id.notification_expanded_play, isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
            mExpandedView.setImageViewBitmap(R.id.widget_image, artImage);
            mExpandedView.setOnClickPendingIntent(R.id.notification_collapse, cancelPending);
            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_prev, prevPending);
            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_next, nextPending);
            mExpandedView.setOnClickPendingIntent(R.id.notification_expanded_play, playPausePending);
            notification.bigContentView = mExpandedView;
        }

        if (mNotificationManager != null && !isClosedFromNotification)
            mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    public void updateNotification(String stationName, String singerName, String songName, int smallImage, int artImage) {
        if(stationName!=null)this.stationName = stationName;
        else this.stationName="";

        if(singerName!=null)this.singerName = singerName;
        else this.singerName="";

        if(songName!=null)this.songName = songName;
        else this.songName = "";

        if(smallImage!=-1)this.smallImage = smallImage;
        if(artImage!=-1)this.artImage = BitmapFactory.decodeResource(getResources(), artImage);
        if(!isClosedFromNotification) buildNotification();
    }


    public void updateNotification(String stationName, String singerName, String songName, int smallImage, Bitmap artImage) {
        if(stationName!=null)this.stationName = stationName;
        else this.stationName="";

        if(singerName!=null)this.singerName = singerName;
        else this.singerName="";

        if(songName!=null)this.songName = songName;
        else this.songName = "";

        if(smallImage!=-1)this.smallImage = smallImage;
        if(artImage!=null)this.artImage = artImage;
        if(!isClosedFromNotification) buildNotification();
    }

    public void setSleepTimer(int seconds){
        if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
        sleepTimerTask=null;
        sleepTimerTask=new SleepTimerTask();
        sleepTimerTask.execute(seconds);
        notifySleepTimerStatusUpdate(SLEEP_TIMER_START,seconds);
        Log.d(TAG, "sleep timer created");
    }
    public void cancelSleepTimer(){
        if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
        notifySleepTimerStatusUpdate(SLEEP_TIMER_CANCEL,-1);
        Log.d(TAG, "sleep timer cancelled");
    }
    public class SleepTimerTask extends AsyncTask<Integer, Integer, Void> {
        @Override
        protected void onProgressUpdate(Integer... values) {
            int secondsLeft = values[0];
            Log.d(TAG, "onProgressUpdate sleep timer left "+ secondsLeft +" seconds");
            notifySleepTimerStatusUpdate(SLEEP_TIMER_UPDATE, secondsLeft);
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "sleep timer finished");
            notifySleepTimerStatusUpdate(SLEEP_TIMER_FINISH, -1);
            if(isPlaying()) stop();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(TAG, "sleep timer cancelled");
            notifySleepTimerStatusUpdate(SLEEP_TIMER_CANCEL, -1);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int secondsLeft = params[0];
            while(secondsLeft>=0) {
                if(secondsLeft%30==0)publishProgress(secondsLeft);
                SystemClock.sleep(1000);
                secondsLeft--;
            }
            return null;
        }
    }
}
