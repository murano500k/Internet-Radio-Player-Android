package com.android.murano500k.onlineradioplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Created by artem on 6/14/16.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    public static final String TAG="RemoteControlReceiver";
    public interface PlaybackControl
        {
            void play();
            void pause();
            void playpause();
            void next();
            void prev();
        }
    PlaybackControl playbackControl;

    public RemoteControlReceiver(PlaybackControl playbackControl) {
        super();
        this.playbackControl=playbackControl;
    }
    public RemoteControlReceiver() {
        super();

    }
    public void setPlaybackControl(RemoteControlReceiver.PlaybackControl pc){
        playbackControl=pc;
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG,"get something. i dont know what!!");

        String intentAction = intent.getAction();
        KeyEvent event = null;
        if (Intent.ACTION_SCREEN_OFF.equals(intentAction)) {
            Log.d(TAG, "RemoteControlReceiver:ACTION_SCREEN_OFF" );
            return;
        }
        /*else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            event = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        }*/
        if(Intent.ACTION_HEADSET_PLUG.equals(intentAction)) {
            int  state = intent
                    .getIntExtra("state", -1);
            if(state==1) {
                Toast.makeText(context, "Headphones plugged, resuming playback", Toast.LENGTH_SHORT).show();
                playbackControl.play();
            }
            if(state==0) {
                Toast.makeText(context, "Headphones UNplugged, pausing playback", Toast.LENGTH_SHORT).show();
                playbackControl.pause();
            }
            return;
        }
/*
        int keycode = event.getKeyCode();
        int action = event.getAction();
        long eventtime = event.getEventTime();

        switch (keycode) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Trigered KEYCODE_VOLUME_UP KEYCODE_MEDIA_NEXT");
                    playbackControl.next();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Trigered KEYCODE_VOLUME_DOWN KEYCODE_MEDIA_PREVIOUS");

                    playbackControl.prev();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Trigered PLAY_PAUSE KEYCODE_HEADSETHOOK");
                    playbackControl.playpause();
                }
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Trigered PAUSE ");
                    playbackControl.pause();

                }
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Trigered PLAY");
                    playbackControl.play();
                }


            default:
                break;
        }*/
        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
        return;
    }
}

