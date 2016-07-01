package com.android.murano500k.onlineradioplayer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements RadioListener, ListSelectListener,
        RemoteControlReceiver.PlaybackControl {
    private static final String KEY_SHUFFLE_STATE = "KEY_SHUFFLE_STATE";
    private static final String KEY_BTN_PLAY_STATE = "KEY_BTN_PLAY_STATE";
    private static final String KEY_SLEEP_TIMER_STATE = "KEY_SLEEP_TIMER_STATE";
    private static final String KEY_SLEEP_TIMER_MINUTES = "KEY_SLEEP_TIMER_MINUTES";
    RecyclerView recyclerView;
    MyStationRecyclerViewAdapter adapter;
    public static RadioManager radioManager;
    ImageButton btnPlay;
    public ImageButton btnPrev, btnNext, btnShuffle;
    private AVLoadingIndicatorView spinner;
    public ImageView networkStatus;
    private static final String TAG = "Main2Activity";
    private MediaSession mediaSession;
    private MediaSession.Token  mediaSessionToken;
    private AudioManager mAudioManager;
    final public static String KEY_SELECTED_INDEX= "KEY_SELECTED_INDEX";
    public RemoteControlReceiver mRemoteControlHandler;
    public TextView sleepMinutes;
    public RelativeLayout sleepLayout;
    public ImageButton btnSleep;
    private boolean sleepTimerRunning;
    private int selectedIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        radioManager = RadioManager.with(this);
        radioManager.connect();
        radioManager.registerListener(this);
        //TODO:check onPause manager update

        initList();
        initMediaSession();
        initButtonListener();
        initSleepTimer();
        selectedIndex=0;
        if(savedInstanceState!=null){
            btnShuffle.setActivated(savedInstanceState.getBoolean(KEY_SHUFFLE_STATE, false));
            switch (savedInstanceState.getInt(KEY_BTN_PLAY_STATE)){
                case 1:
                    btnPlay.setActivated(true);
                    break;
                case -1:
                    btnPlay.setActivated(false);
                    break;
            }
            selectedIndex=savedInstanceState.getInt(KEY_SELECTED_INDEX, 0);
            //sleepTimerRunning=savedInstanceState.getBoolean(KEY_SLEEP_TIMER_STATE, false);
            //if(sleepTimerRunning) updateSleepTimer(savedInstanceState.getInt(KEY_SELECTED_INDEX, 0));
            //else enableSleepButtonSet();
        }
        radioManager.setCurrentStation(StationContent.STATION_LIST.get(selectedIndex));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
         if(RadioPlayerService.NOTIFICATION_INTENT_OPEN_PLAYER.equals(action)){
        Log.d(TAG, " NOTIFICATION_INTENT_OPEN_PLAYER called");
        }
        else if(RadioPlayerService.WIDGET_INTENT_NEXT.equals(action)){
        Log.d(TAG, " WIDGET_INTENT_NEXT called");
            pause();
            next();
        }
        else if(RadioPlayerService.WIDGET_INTENT_PREV.equals(action)){
        Log.d(TAG, " WIDGET_INTENT_PREV called");
            pause();
            prev();
        }
        else if(RadioPlayerService.WIDGET_INTENT_PLAY_PAUSE.equals(action)){
        Log.d(TAG, " WIDGET_INTENT_PLAY_PAUSE called");
            playpause();
        }
        else if(RadioPlayerService.NOTIFICATION_INTENT_CANCEL.equals(action)) {
            Log.d(TAG, "activ NOTIFICATION_INTENT_CANCEL called");
            if (radioManager != null) {
                radioManager.setClosedFromNotification(true);
            }
            finish();

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (radioManager.getCurrentStation() != null)
            outState.putInt(KEY_SELECTED_INDEX, Integer.valueOf(radioManager.getCurrentStation().id));
        outState.putBoolean(KEY_SHUFFLE_STATE, btnShuffle.isActivated());
        outState.putInt(KEY_BTN_PLAY_STATE, btnPlay.isActivated() ? -1 : 1);
        outState.putBoolean(KEY_SLEEP_TIMER_STATE, sleepTimerRunning);
        if (sleepTimerRunning) {
            String i = sleepMinutes.getText().toString();
            if (!i.contains("n")) outState.putInt(KEY_SLEEP_TIMER_MINUTES, Integer.parseInt(i));
        }

    }
    public void initButtonListener(){
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlHandler = new RemoteControlReceiver();
        mRemoteControlHandler.setPlaybackControl(this);
        IntentFilter filter= new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mRemoteControlHandler, filter);
    }

    public void initMediaSession(){
        String TAG="MyMediaSession";
        mediaSession = new MediaSession(getApplicationContext(), TAG);

        if (mediaSession == null) {
            Log.e(TAG, "initMediaSession: mediaSession = null");
            return;
        }

        mediaSessionToken = mediaSession.getSessionToken();

        mediaSession.setCallback(new MediaSession.Callback() {
            String TAG="MyMediaSession";

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
            }

            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.d(TAG, "onMediaButtonEvent");

                if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                    KeyEvent event = (KeyEvent)mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    int keycode = event.getKeyCode();
                    int action = event.getAction();
                    long eventtime = event.getEventTime();
                switch (keycode) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            if (action == KeyEvent.ACTION_DOWN) {
                                Log.d(TAG, "Trigered KEYCODE_VOLUME_UP KEYCODE_MEDIA_NEXT");
                                next();
                            }
                            break;
                    //case KeyEvent.KEYCODE_VOLUME_UP:
                    //case KeyEvent.KEYCODE_VOLUME_DOWN:

                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            if (action == KeyEvent.ACTION_DOWN) {
                                Log.d(TAG, "Trigered KEYCODE_VOLUME_DOWN KEYCODE_MEDIA_PREVIOUS");

                                prev();
                            }
                            break;
                        /*case KeyEvent.KEYCODE_HEADSETHOOK:
                            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                            audioManager.isWiredHeadsetOn()
                            if (action == ) {
                                Log.d(TAG, "Trigered PLAY_PAUSE KEYCODE_HEADSETHOOK");
                                playpause();
                            }*/
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            if (action == KeyEvent.ACTION_DOWN) {
                                Log.d(TAG, "Trigered PLAY_PAUSE KEYCODE_HEADSETHOOK");
                                playpause();
                            }
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            if (action == KeyEvent.ACTION_DOWN) {
                                Log.d(TAG, "Trigered PAUSE ");
                                pause();

                            }
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            if (action == KeyEvent.ACTION_DOWN) {
                                Log.d(TAG, "Trigered PLAY");
                                play();
                            }
                            break;


                        default:
                            break;
                    }
                }
                Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
                return super.onMediaButtonEvent(mediaButtonIntent);
            }

            public void onPause() {
                Log.d(TAG, "onPause called (media button pressed)");
                super.onPause();
            }

            public void onPlay() {
                Log.d(TAG, "onPlay called (media button pressed)");
                super.onPlay();
            }

            public void onStop() {
                Log.d(TAG, "onStop called (media button pressed)");
                super.onStop();
            }
        });

        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);


    }

    public void resetMediaSession(){
        if(mediaSession!=null) mediaSession.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetMediaSession();
        unregisterReceiver(mRemoteControlHandler);
        if(radioManager!=null)radioManager.unregisterListener(this);
        if(radioManager!=null)radioManager.disconnect();

    }
    public void initList(){
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter=new MyStationRecyclerViewAdapter(StationContent.STATION_LIST,this, getApplicationContext());
        recyclerView.setAdapter(adapter);
        adapter.setRecyclerView(recyclerView);
    }
    public void initializeUI() {

        networkStatus = (ImageView) findViewById(R.id.networkStatus);
        btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isActivated()) {
                    radioManager.stopRadio();
                } else radioManager.startRadio();
            }
        });
        spinner=(AVLoadingIndicatorView) findViewById(R.id.spinner);
        btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        btnShuffle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_BUTTON_PRESS){
                    v.setPressed(true);
                } else if(event.getAction()==MotionEvent.ACTION_BUTTON_RELEASE){
                    v.setPressed(false);
                }
                return false;
            }
        });
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isActivated()) {
                    v.setActivated(false);
                }else {
                    v.setActivated(true);
                }
            }
        });
        btnPrev= (ImageButton) findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prev();
            }
        });
        btnNext= (ImageButton) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });


    }

    @Override
    public void stationSelected(StationContent.Station station) {
        Log.d(TAG,"stationSelected");
        radioManager.stationSelected(station);
        updateWidget();
    }


    public void updateWidget()  {
        Intent intent = new Intent(this, SimpleWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), SimpleWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        intent.putExtra(SimpleWidgetProvider.SELECTED_STATION_TITLE, radioManager.getCurrentStation().name);
        intent.putExtra(SimpleWidgetProvider.IS_PLAYING, radioManager.isPlaying());
        sendBroadcast(intent);
    }

    @Override
    public void play() {
        radioManager.startRadio();
    }

    @Override
    public void pause() {
        radioManager.stopRadio();
    }

    @Override
    public void playpause() {
        if(radioManager.isPlaying()) {
            radioManager.stopRadio();
        } else radioManager.startRadio();
    }

    @Override
    public void next() {
        int currentIndex;
        if(btnShuffle.isActivated()){
            currentIndex= new Random().nextInt(StationContent.STATION_LIST.size()-1);
        }else {
            int lastIndex= Integer.valueOf(radioManager.getCurrentStation().id);
            if(lastIndex<StationContent.STATION_LIST.size()-2){
                currentIndex=lastIndex+1;
            }else currentIndex=0;
        }
        radioManager.stationSelected(StationContent.STATION_LIST.get(currentIndex));
    }

    @Override
    public void prev() {
        int currentIndex;
        int lastIndex= Integer.valueOf(radioManager.getCurrentStation().id);
        if(lastIndex>0){
            currentIndex=lastIndex-1;
        }else currentIndex=0;
        radioManager.stationSelected(StationContent.STATION_LIST.get(currentIndex));
    }


    @Override
    public void onNextSongShouldPlay() {
        Log.d(TAG,"activity onNextSongShouldPlay");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                next();
            }
        });

    }
    @Override
    public void onPrevSongShouldPlay() {
        Log.d(TAG,"activity onPrevSongShouldPlay");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prev();

            }
        });

    }

    @Override
    public void onCurrentSongShouldPlay() {
        Log.d(TAG,"activity onCurrentSongShouldPlay");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                play();
            }
        });
    }


    @Override
    public void onError() {
        Log.d(TAG,"onError");

        //needResume =true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlay.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
                PlaybackState state = new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY)
                        .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, SystemClock.elapsedRealtime())
                        .build();
                if(mediaSession!=null) {
                    mediaSession.setPlaybackState(state);
                    mediaSession.setActive(false);
                }

                if(!radioManager.isClosedFromNotification()) radioManager.updateNotification(radioManager.getCurrentStation().name,null,null,-1,-1);
                updateWidget();
            }
        });
    }

    @Override
    public void onRadioLoading() {
        Log.d(TAG,"onRadioLoading");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //btnPlay.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                updateWidget();
            }
        });
    }

    @Override
    public void onRadioConnected() {
        Log.d(TAG,"onRadioConnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //initNM();
            }
        });
    }

    @Override
    public void onRadioStarted() {
        Log.d(TAG,"onRadioStarted");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.updateAnimState(true);
                btnPlay.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
                btnPlay.setActivated(true);
                //playInd.setVisibility(View.VISIBLE);
                adapter.setSelectedIndex(Integer.valueOf(radioManager.getCurrentStation().id));
                PlaybackState state = new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY)
                        .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, SystemClock.elapsedRealtime())
                        .build();
                if(mediaSession!=null) {
                    mediaSession.setPlaybackState(state);
                    mediaSession.setActive(true);
                }
                radioManager.updateNotification(radioManager.getCurrentStation().name,null,null,-1,-1);
                updateWidget();
            }
        });

    }

    @Override
    public void onRadioStopped(final boolean updateNotification) {
        Log.d(TAG,"onRadioStopped");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlay.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
                btnPlay.setActivated(false);
                //playInd.setVisibility(View.GONE);
                PlaybackState state = new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY)
                        .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, SystemClock.elapsedRealtime())
                        .build();
                if(mediaSession!=null) {
                    mediaSession.setPlaybackState(state);
                    mediaSession.setActive(false);
                }
                adapter.updateAnimState(false);
                if(updateNotification) radioManager.updateNotification(null,null,null,-1,-1);
                updateWidget();
                //needResume =false;
            }
        });

    }



    @Override
    public void onMetaDataReceived(String s, String s2) {
        //TODO: UPDATE NOTIF and ALBUMART
        Log.d(TAG,"onMetaDataReceived");

        Log.d(TAG, "s = "+ s+ "\ts2 = "+ s2);
        final String data=s2;
        if(s!=null && s.equals("StreamTitle")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setPlayingInfo(getArtistFromString(data),getTrackFromString(data));
                    radioManager.updateNotification(radioManager.getCurrentStation().name,getArtistFromString(data),getTrackFromString(data),-1,-1);
                    updateWidget();
                }
            });
        }
    }


    @Override
    public void onSleepTimerStatusUpdate(final String action ,final int seconds) {
        Log.d(TAG,"onSleepTimerStatusUpdate " + action);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(action.equals(RadioPlayerService.SLEEP_TIMER_START)) {
                    Log.d(TAG,"Sleep timer started. Minutes left: "+ seconds/60);
                    Toast.makeText(getApplicationContext(),"Sleep timer started. Minutes left: "+ seconds/60 ,Toast.LENGTH_LONG).show();
                    enableSleepButtonCancel();
                    updateSleepTimer(seconds);
                }
                else if(action.equals(RadioPlayerService.SLEEP_TIMER_CANCEL)) {
                    enableSleepButtonSet();
                    Log.d(TAG,"Sleep timer cancelled. Pausing playback");
                    Toast.makeText(getApplicationContext(),
                            "Sleep timer cancelled. Pausing playback",Toast.LENGTH_LONG).show();
                }
                else if(action.equals(RadioPlayerService.SLEEP_TIMER_FINISH)) {
                    enableSleepButtonSet();
                    Log.d(TAG,"Sleep timer finished. Pausing playback");
                    Toast.makeText(getApplicationContext(),
                            "Sleep timer finished. Pausing playback",Toast.LENGTH_LONG).show();
                }
                else if(action.equals(RadioPlayerService.SLEEP_TIMER_UPDATE)) {
                    updateSleepTimer(seconds);
                    Log.d(TAG,"Sleep timer updated. Minutes left: "+ seconds/60);
                }
            }
        });

    }

    public void initSleepTimer(){
        btnSleep = (ImageButton) findViewById(R.id.btnSleep);
        sleepLayout = (RelativeLayout) findViewById(R.id.sleepTimerLayout);
        sleepMinutes = (TextView) findViewById(R.id.sleepMinutes);
        sleepLayout.setVisibility(View.GONE);
        btnSleep.setVisibility(View.VISIBLE);
        btnSleep.setOnClickListener(setSleepClickListener);
    }
    public void updateSleepTimer(int seconds) {
        if(sleepLayout.getVisibility()!=View.VISIBLE){
            enableSleepButtonCancel();
        }
        int minutes = seconds/60;
        if(minutes==0){
            Log.d(TAG,"updateSleepTimer < 1 minute left"+ seconds/60);
            sleepMinutes.setText("< 1");
        }else {
            Log.d(TAG,"updateSleepTimer minutes = "+ seconds/60);
            sleepMinutes.setText(String.valueOf(seconds/60));
        }

    }
    View.OnClickListener setSleepClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "btnSleep clicked");
            showSetTimerDialog();
        }
    };
    View.OnClickListener cancelSleepClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "btnSleep clicked");
            showCancelTimerDialog();
        }
    };
    public void enableSleepButtonCancel() {
        sleepLayout.setVisibility(View.VISIBLE);
        btnSleep.setImageResource(R.drawable.ic_no_sleep);
        btnSleep.setOnClickListener(cancelSleepClickListener);

    }
    public void enableSleepButtonSet() {
        sleepLayout.setVisibility(View.GONE);
        btnSleep.setImageResource(R.drawable.ic_sleep);
        btnSleep.setOnClickListener(setSleepClickListener);
    }

    private void showCancelTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog =builder.setTitle("Cancel current timer?")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                Log.d(TAG,"try to cancel timer");

                                if(radioManager!=null && RadioManager.getService()!=null) {
                                    Intent intent = new Intent();
                                    intent.setAction(RadioPlayerService.SLEEP_INTENT_CANCEL_TIMER);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);

                                    PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
                                    try {
                                        pending.send();
                                    } catch (PendingIntent.CanceledException e) {
                                        Log.d(TAG, "PendingIntent.CanceledException e: "+ e.getMessage() );
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                Log.d(TAG,"cancelled");
                            }
                        }).create();
        dialog.show();

    }
    public int selected;
    private void showSetTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CharSequence[] array = {"1", "15", "30", "60", "90", "120", "240"};
        AlertDialog dialog =builder.setTitle("Set sleep timer in minutes")
                .setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selected = which;
                    }
                })
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                Log.d(TAG,"try to set timer");

                                int mins=Integer.parseInt(array[selected].toString());
                                int secondsBeforeSleep=mins*60;
                                if(radioManager!=null && RadioManager.getService()!=null) {
                                    Intent intent = new Intent();
                                    intent.setAction(RadioPlayerService.SLEEP_INTENT_SET_TIMER);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    Log.d(TAG,"secondsBeforeSleep putExtra : " + secondsBeforeSleep);
                                    intent.putExtra(RadioPlayerService.LEFT_SECONDS, secondsBeforeSleep);
                                    PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
                                    try {
                                        pending.send();
                                    } catch (PendingIntent.CanceledException e) {
                                        Log.d(TAG, "PendingIntent.CanceledException e: "+ e.getMessage() );
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                Log.d(TAG,"cancelled");
                            }
                        }).create();
        dialog.show();
    }
    public String getArtistFromString(String data){
        String artistName="";
        if(data!=null && data.contains(" - ")) {
            artistName=data.substring(0, data.indexOf(" - "));
        }
        Log.d(TAG,"getArtistFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"artistNameStr: "+ artistName);

        return artistName;
    }
    public String getTrackFromString(String data){
        String trackName="";
        if(data!=null && data.contains(" - ")) {
            trackName=data.substring(data.indexOf(" - ")+3);
        }
        Log.d(TAG,"getTrackFromString");
        Log.d(TAG,"dataStr: "+ data);
        Log.d(TAG,"trackName: "+ trackName);
        return trackName;
    }



}