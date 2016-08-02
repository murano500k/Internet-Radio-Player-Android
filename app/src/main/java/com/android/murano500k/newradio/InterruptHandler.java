package com.android.murano500k.newradio;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.cantrowitz.rxbroadcast.RxBroadcast;

import java.util.ArrayList;

import rx.Subscription;

import static android.telephony.PhoneStateListener.LISTEN_NONE;

/**
 * Created by artem on 7/25/16.
 */
public class InterruptHandler implements ListenerRadio{
	private static final String TAG = "InterruptHandler";
	private AudioManager audioManager;
	private TelephonyManager mTelephonyManager;

	AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
	PhoneStateListener phoneStateListener ;



	private boolean isInterrupted;
	ServiceRadio serviceRadio;
	private NotificationManager mNotificationManager;
	private Context context;
	private boolean isConnected;
	private ConnectivityManager cm;
	private Subscription connectedSubscription;

	private AudioDeviceCallback audioDeviceCallback;

	public boolean isInterrupted() {
		return isInterrupted;
	}

	public void setInterrupted(boolean interrupted) {
		Log.d(TAG, "isInterrupted = "+ interrupted);

		isInterrupted = interrupted;
	}

	public InterruptHandler(final Context context, final ServiceRadio serviceRadio){
		this.serviceRadio=serviceRadio;
		this.context=context;
		phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
					if (serviceRadio.isPlaying()) {
						isInterrupted = true;
                        /*if(networkChangeReceiver!=null && networkChangeReceiver.isListening){
                            context.unregisterReceiver(networkChangeReceiver);
                            networkChangeReceiver.isListening=false;
                        }*/
						serviceAction(Constants.INTENT_PAUSE_PLAYBACK);
					}
				} else if (state == TelephonyManager.CALL_STATE_IDLE) {
					if (isInterrupted) {
						serviceAction(Constants.INTENT_RESUME_PLAYBACK);
					}
				}
				super.onCallStateChanged(state, incomingNumber);
			}
		};
		mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			audioDeviceCallback = new AudioDeviceCallback() {
				@Override
				public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
					super.onAudioDevicesAdded(addedDevices);
				}
				@Override
				public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
					for (AudioDeviceInfo info :
							removedDevices) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
							if (AudioDeviceInfo.TYPE_WIRED_HEADSET == info.getType()
									|| AudioDeviceInfo.TYPE_WIRED_HEADPHONES == info.getType()) {
								if(connectedSubscription != null && !connectedSubscription.isUnsubscribed()) resetConnectivityListener();
								if (mTelephonyManager != null && phoneStateListener!=null)
									mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
								if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);
								super.onAudioDevicesRemoved(removedDevices);
								return;
							}
						}
					}
					super.onAudioDevicesRemoved(removedDevices);
				}
			};
		}

		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
			public void onAudioFocusChange(int focusChange) {
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
						focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
				{
					if (serviceRadio.isPlaying()) {
						isInterrupted = true;
						audioManager.abandonAudioFocus(audioFocusChangeListener);
						serviceAction(Constants.INTENT_PAUSE_PLAYBACK);

					}
				} else if(focusChange == AudioManager.AUDIOFOCUS_GAIN ||
						focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
				{
					if (isInterrupted) {
						serviceAction(Constants.INTENT_RESUME_PLAYBACK);
					}
				}
			}
		};

	}



	public boolean requestFocus(){
		if (audioManager == null) audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		boolean granted=false;
		int result = audioManager.requestAudioFocus(audioFocusChangeListener,
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);

		granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			//audioManager.registerAudioDeviceCallback(audioDeviceCallback);
			audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
		}
		Log.d(TAG, "requested audio focus. result = "+ granted);
		return granted;
	}



	private void serviceAction(String action){
		Intent intent=new Intent(action);
		intent.setClass(context, ServiceRadio.class);
		context.startService(intent);
	}
	private void registerConnectivityListener(){
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		connectedSubscription =RxBroadcast.fromBroadcast(context, filter)
				.subscribe(this::handleConnectivityChange);
		Log.d(TAG, "connectedSubscription registered");

	}
	private void resetConnectivityListener(){
		if(connectedSubscription!=null && !connectedSubscription.isUnsubscribed()) {
			connectedSubscription.unsubscribe();
			Log.d(TAG, "connectedSubscription reset");

		}

	}
	void handleConnectivityChange(final Intent intent) {
		isConnected=isOnline(context);
		Log.d(TAG, "ConnectivityListener handleConnectivityChange. isOnline: "+  isConnected);
		if(isConnected) serviceAction(Constants.INTENT_HANDLE_CONNECTIVITY);

	}
	boolean isOnline(Context context) {
		if (cm == null) cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if(netInfo != null && netInfo.isConnected()){
			return true;
		}else {
			return false;
		}
	}

	@Override
	public void onLoadingStarted(String url) {
		if(!isOnline(context)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					SystemClock.sleep(3000);
					if(!isOnline(context)) serviceAction(Constants.INTENT_PAUSE_PLAYBACK);
				}
			}).start();
		}
	}

	@Override
	public void onRadioConnected() {

	}

	@Override
	public void onPlaybackStarted(String url) {
		registerConnectivityListener();
		if (mTelephonyManager != null && phoneStateListener!=null)
			mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onPlaybackStopped(boolean updateNotification) {
		resetConnectivityListener();
		if (mTelephonyManager != null && phoneStateListener!=null)
			mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
		if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);

	}

	@Override
	public void onMetaDataReceived(String s, String s2) {

	}

	@Override
	public void onPlaybackError() {
		if(connectedSubscription.isUnsubscribed()) registerConnectivityListener();
		if (mTelephonyManager != null && phoneStateListener!=null)
			mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
		if(audioFocusChangeListener!=null)audioManager.abandonAudioFocus(audioFocusChangeListener);

	}

	@Override
	public void onListChanged(ArrayList<Station> newlist) {

	}

	@Override
	public void onStationSelected(String url) {

	}

	@Override
	public void onSleepTimerStatusUpdate(String action, int seconds) {
		if(!action.contains(Constants.INTENT_SLEEP_TIMER_CANCEL) && seconds<3) {
			resetConnectivityListener();
			if (mTelephonyManager != null && phoneStateListener!=null)
				mTelephonyManager.listen(phoneStateListener, LISTEN_NONE);
		}
	}
/*
    public void listenConnectivity() {
        if(networkChangeReceiver==null) networkChangeReceiver= new NetworkChangeReceiver();
        if(!networkChangeReceiver.isListening ) {
            context.registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            networkChangeReceiver.isListening=true;
        }
    }
    public void dontListenConnectivity() {
        if(networkChangeReceiver==null) networkChangeReceiver= new NetworkChangeReceiver();
        if(networkChangeReceiver.isListening) {
            context.unregisterReceiver(networkChangeReceiver);
            networkChangeReceiver.isListening=false;
        }
    }*/
    /*public void initRxMediaSession() {
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_MEDIA_BUTTON);
        filter.setPriority(  IntentFilter.SYSTEM_HIGH_PRIORITY-1);


        mediaButtonObservable = RxBroadcast.fromBroadcast(context, filter)
                .subscribe(this::onVolumeButtonEvent);


    }*/

    /*public void initMediaSession(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String TAG="MyMediaSession";
            mediaSession = new MediaSession(context, TAG);

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
registerConnectivityListener();
        if (mTelephonyManager != null && phoneStateListener!=null)
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                @Override
                public void onSkipToPrevious() {
                    super.onSkipToPrevious();
                }

                public boolean onMediaButtonEvent(Intent intent) {
                    Log.d(TAG, "onMediaButtonEvent");

                    if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                        KeyEvent event= intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        int keycode = event.getKeyCode();
                        int keyAction = event.getAction();
                        switch (keycode) {
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                if (keyAction == KeyEvent.ACTION_DOWN) {
                                    Log.d(TAG, "KEYCODE_MEDIA_NEXT");
                                    serviceAction(Constants.INTENT_PLAY_NEXT);
                                }
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                if (keyAction == KeyEvent.ACTION_DOWN) {
                                    Log.d(TAG, "KEYCODE_MEDIA_PREVIOUS");
                                    serviceAction(Constants.INTENT_PLAY_PREV);
                                }
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                if (keyAction == KeyEvent.ACTION_DOWN) {
                                    Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY_PAUSE");
                                    serviceAction(Constants.INTENT_PLAY_PAUSE);
                                }
                            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                if (keyAction == KeyEvent.ACTION_DOWN) {
                                    Log.d(TAG, "Trigered KEYCODE_MEDIA_PAUSE ");
                                    serviceAction(Constants.INTENT_PAUSE_PLAYBACK);
                                }
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                                if (keyAction == KeyEvent.ACTION_DOWN) {
                                    Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY");
                                    serviceAction(Constants.INTENT_RESUME_PLAYBACK);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    Log.d(TAG, "onMediaButtonEvent called: " + intent);
                    return super.onMediaButtonEvent(intent);
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

    }*/



/*
    public boolean onVolumeButtonEvent(@NonNull Intent mediaButtonIntent) {
        Log.d(TAG, "onVolumeButtonEvent "+ mediaButtonIntent.getDataString());

        Intent i = mediaButtonIntent;
        KeyEvent event= mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if(event==null) return true;
            int keycode = event.getKeyCode();
            int action = event.getAction();
            int flags = event.getFlags();

            long eventtime = event.getEventTime();
            switch (keycode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    Log.d(TAG, "Trigered KEYCODE_VOLUME_UP");

                    if (flags == KeyEvent.FLAG_LONG_PRESS) {
                        Log.d(TAG, "Trigered KEYCODE_VOLUME_UP KEYCODE_MEDIA_NEXT");
                        serviceAction(Constants.INTENT_PLAY_NEXT);
                    }
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (action == KeyEvent.FLAG_LONG_PRESS) {
                        Log.d(TAG, "Trigered KEYCODE_VOLUME_UP KEYCODE_MEDIA_NEXT");
                        serviceAction(Constants.INTENT_PLAY_NEXT);
                    }
                    break;

                default:
                    break;
            }
        Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
        return true;
    }
*/




   /* public class NetworkChangeReceiver extends BroadcastReceiver {
        private static final String TAG = "NetworkChangeReceiver";
        ConnectivityManager cm;
        public boolean isConnected;
        public boolean isListening;

        @Override
        public void onReceive(final Context context, final Intent intent) {
            boolean isOnline=isOnline(context);
            Log.d(TAG, "NetworkChangeReceiver onReceive. isOnline: "+ isOnline );
            if(isOnline) {
                isConnected=true;
            } else {
                isConnected=false;
            }
            Log.d(TAG, "network changed. Status connected: "+ isConnected
                    + ", is playing: " + serviceRadio.isPlaying()
                    + ", is  interupted: " + isInterrupted);
            if(isConnected && isInterrupted && !serviceRadio.isPlaying()) {
                Toast.makeText(context, "Network connected = "+isConnected +". Resume playback", Toast.LENGTH_SHORT).show();
                serviceAction(Constants.INTENT_RESUME_PLAYBACK);
            }

        }
        public boolean isOnline(Context context) {
            if (cm == null) cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if(netInfo != null && netInfo.isConnected()){
                return true;
            }else {
                return false;
            }
        }
    }*/
}


