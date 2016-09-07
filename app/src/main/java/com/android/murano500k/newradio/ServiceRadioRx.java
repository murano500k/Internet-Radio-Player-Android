package com.android.murano500k.newradio;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.android.murano500k.newradio.ui.NotificationProviderRx;
import com.android.murano500k.newradio.ui.PlayerStatus;
import com.android.murano500k.newradio.ui.SleepEvent;
import com.android.murano500k.newradio.ui.UiEvent;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Random;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;



public class ServiceRadioRx extends Service implements PlayerCallback{
	public static final String INTENT_RESET = "com.android.murano500k.newradio.INTENT_RESET";
	public static final String INTENT_SLEEP_SET = "com.android.murano500k.newradio.INTENT_SLEEP_SET";
	public static final String INTENT_SLEEP_CANCEL = "com.android.murano500k.newradio.INTENT_SLEEP_CANCEL";
	public static final String INTENT_SET_BUFFER_SIZE = "com.android.murano500k.newradio.INTENT_SET_BUFFER_SIZE";

	public static final String EXTRA_SLEEP_SECONDS= "com.android.murano500k.newradio.INTENT_SLEEP_CANCEL";

	public static final String INTENT_USER_ACTION = "com.android.murano500k.newradio.INTENT_USER_ACTION";
	public static final String EXTRA_PLAY_PAUSE_PRESSED = "com.android.murano500k.newradio.EXTRA_PLAY_PAUSE_PRESSED";
	public static final String EXTRA_NEXT_PRESSED = "com.android.murano500k.newradio.EXTRA_NEXT_PRESSED";
	public static final String EXTRA_PREV_PRESSED = "com.android.murano500k.newradio.EXTRA_PREV_PRESSED";
	public static final String EXTRA_PLAY_LIST_STATION_PRESSED = "com.android.murano500k.newradio.EXTRA_PLAY_LIST_STATION_PRESSED";
	public static final String EXTRA_URL = "com.android.murano500k.newradio.EXTRA_URL";

	public static final PlayerStatus STATUS_PLAYING= new PlayerStatus("STATUS_PLAYING", 1, LOADING_TYPE.NOT_LOADING, FOCUS_STATUS.HAS_FOCUS);
	public static final PlayerStatus STATUS_STOPPED= new PlayerStatus("STATUS_STOPPED", -1, LOADING_TYPE.NOT_LOADING, FOCUS_STATUS.FOCUS_NOT_REQUESTED);
	public static final PlayerStatus STATUS_WAITING_CONNECTIVITY= new PlayerStatus("STATUS_WAITING_CONNECTIVITY", 0, LOADING_TYPE.WAITING_CONNECTIVYTY, FOCUS_STATUS.FOCUS_NOT_REQUESTED);
	public static final PlayerStatus STATUS_WAITING_FOCUS= new PlayerStatus("STATUS_WAITING_FOCUS", 0, LOADING_TYPE.WAITING_FOCUS, FOCUS_STATUS.WAITING_FOR_FOCUS);
	public static final PlayerStatus STATUS_WAITING_UNMUTE= new PlayerStatus("STATUS_WAITING_UNMUTE", 0, LOADING_TYPE.WAITING_UNMUTE, FOCUS_STATUS.MUTED);
	public static final PlayerStatus STATUS_LOADING= new PlayerStatus("STATUS_LOADING", 0, LOADING_TYPE.LOADING_STREAM, FOCUS_STATUS.HAS_FOCUS);
	public static final PlayerStatus STATUS_SWITCHING= new PlayerStatus("STATUS_SWITCHING", 1, LOADING_TYPE.SWITCHING_STREAM, FOCUS_STATUS.HAS_FOCUS);

	public enum FOCUS_STATUS {
		HAS_FOCUS,
		MUTED,
		WAITING_FOR_FOCUS,
		FOCUS_NOT_REQUESTED
	}
	public enum LOADING_TYPE{
		NOT_LOADING,
		LOADING_STREAM,
		SWITCHING_STREAM,
		WAITING_CONNECTIVYTY,
		WAITING_FOCUS,
		WAITING_UNMUTE
	}
	public static final int PLAYING=1;
	public static final int LOADING=0;
	public static final int STOPPED=-1;
	private ComponentName mMediaButtonReceiverComponent;
	private SleepTimerTask sleepTimerTask;

	private final String TAG= "ServiceRadioRx";
	public final IBinder mLocalBinder = new LocalBinder();
	private AudioManager audioManager;
	private EventBus bus=EventBus.getDefault();
	private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
	private HeadsetPlugReceiver headsetPlugReceiver;
	private Intent headsetPlugIntent;
	private NotificationProviderRx notificationProviderRx;
	public boolean isServiceConnected;
	private Toast toast;
	private Subscription internetListener;
	private PlaylistManager playlistManager;
	private int AUDIO_BUFFER_CAPACITY_MS = 800;
	private int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";

	private MultiPlayer mRadioPlayer;

	private PlayerStatus playerStatus;

	public boolean isPlaying() {
		return playerStatus==STATUS_PLAYING;
	}

	public class LocalBinder extends Binder {
		public ServiceRadioRx getService() {
			return ServiceRadioRx.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

		notificationProviderRx =new NotificationProviderRx(getApplicationContext(), this);
		registerHeadsetPlugReceiver();
		registerMediaButtonsReciever();
		isServiceConnected=true;
		playerStatus=STATUS_STOPPED;
		notificationProviderRx.updateNotification(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus);

		return mLocalBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		isServiceConnected=false;
		if(notificationProviderRx!=null){
			if(bus.isRegistered(notificationProviderRx)) bus.unregister(notificationProviderRx);
			notificationProviderRx.cancelNotification();
			notificationProviderRx=null;
		}
		if(bus.isRegistered(this)) bus.unregister(this);
		dontlistenConnectivity();
		unRegisterHeadsetPlugReceiver();
		unRegisterMediaButtonsReciever();
		isServiceConnected=false;
		if(playerStatus!=STATUS_STOPPED) {
			if(playerStatus!=STATUS_WAITING_CONNECTIVITY) abandonFocus();
			else dontlistenConnectivity();
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus));
			getPlayer().stop();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		playlistManager=new PlaylistManager(getApplicationContext());
		int buf=playlistManager.getBufferSize();
		int dec=playlistManager.getDecodeSize();
		if(buf>100) AUDIO_BUFFER_CAPACITY_MS=buf;
		if(dec>100) AUDIO_DECODE_CAPACITY_MS=dec;
		showToast("AUDIO_BUFFER_CAPACITY_MS="+ AUDIO_BUFFER_CAPACITY_MS+ "\nAUDIO_DECODE_CAPACITY_MS="+AUDIO_DECODE_CAPACITY_MS );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent!=null){
			String action = intent.getAction();
			if(action!=null && action.contains(INTENT_USER_ACTION)){
				Log.i(TAG, "intent: "+ intent.getExtras().getString(EXTRA_PLAY_PAUSE_PRESSED, "")
						+" "+intent.getExtras().getString(EXTRA_NEXT_PRESSED, "")
						+" "+intent.getExtras().getString(EXTRA_PREV_PRESSED, "")
						+" "+intent.getExtras().getString(EXTRA_PLAY_LIST_STATION_PRESSED, ""));

				if(intent.getExtras().containsKey(EXTRA_PLAY_PAUSE_PRESSED))
					playPausePressed();
				else if (intent.getExtras().containsKey(EXTRA_NEXT_PRESSED))
					playUrlPressed(getNextUrl(playlistManager.getSelectedUrl(), playlistManager));
				else if (intent.getExtras().containsKey(EXTRA_PREV_PRESSED))
					playUrlPressed(getPrevUrl(playlistManager.getSelectedUrl(), playlistManager));
				else if (intent.getExtras().containsKey(EXTRA_PLAY_LIST_STATION_PRESSED))
					playUrlPressed(intent.getStringExtra(EXTRA_URL));

			}else if(action.contains(INTENT_SET_BUFFER_SIZE)) {
				AUDIO_BUFFER_CAPACITY_MS=intent.getIntExtra(Constants.DATA_AUDIO_BUFFER_CAPACITY, 800);
				AUDIO_DECODE_CAPACITY_MS=intent.getIntExtra(Constants.DATA_AUDIO_DECODE_CAPACITY, 400);
				playlistManager.saveBufferSize(AUDIO_BUFFER_CAPACITY_MS);
				playlistManager.saveDecodeSize(AUDIO_DECODE_CAPACITY_MS);
				showToast("AUDIO_BUFFER_CAPACITY_MS="+ AUDIO_BUFFER_CAPACITY_MS+ "\nAUDIO_DECODE_CAPACITY_MS="+AUDIO_DECODE_CAPACITY_MS );
				if(playerStatus!=STATUS_STOPPED) {
					playerStatus=STATUS_LOADING;
					bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
					getPlayer().stop();
				}

			}else if(action.contains(INTENT_RESET)){
				bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
				playerStatus=STATUS_STOPPED;
				abandonFocus();
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
				getPlayer().stop();
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
				showToast("Player reset");
			} else if (action.equals(INTENT_SLEEP_SET)) {
				Log.d(TAG, "serv INTENT_SLEEP_SET");
				int secs = intent.getIntExtra(EXTRA_SLEEP_SECONDS, -254);
				if(secs==-254)             Log.d(TAG, "secs ERROR");
				Log.d(TAG, "secs" + secs);
				setSleepTimer(secs);
			}else if (action.equals(INTENT_SLEEP_CANCEL)) {
				Log.d(TAG, "serv CANCEL");
				cancelSleepTimer();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void playPausePressed() {
		Log.w("playPausePressed", ""+playerStatus);
		if(playerStatus==STATUS_STOPPED){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_CONNECTIVITY) {
			playerStatus=STATUS_STOPPED;
			dontlistenConnectivity();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
		}
		else if(playerStatus==STATUS_WAITING_UNMUTE) {
			Log.e(TAG, "STATUS_WAITING_UNMUTE playPausePressed");
			showToast("STATUS_WAITING_UNMUTE");
		}
		else if(playerStatus==STATUS_SWITCHING) {
			Log.e(TAG, "STATUS_SWITCHING playPausePressed");
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			getPlayer().stop();
		}
		else if(playerStatus==STATUS_WAITING_FOCUS) {
			playerStatus=STATUS_STOPPED;
			abandonFocus();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
		} else if(playerStatus==STATUS_LOADING){
			Log.e(TAG, "STATUS_LOADING playPausePressed");
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			getPlayer().stop();

		} else if(playerStatus==STATUS_PLAYING){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			getPlayer().stop();
		}
	}
	private void playUrlPressed(String url) {
		Log.w("playUrlPressed", ""+playerStatus);

		if(playerStatus==STATUS_WAITING_UNMUTE) {
			Log.e(TAG, "STATUS_WAITING_UNMUTE playPausePressed");
			showToast("STATUS_WAITING_UNMUTE");
		} if(playerStatus==STATUS_STOPPED){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			updateUrl(url);
			if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,
						playerStatus, playlistManager.getSelectedUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_CONNECTIVITY) {
			playerStatus=STATUS_STOPPED;
			dontlistenConnectivity();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			updateUrl(url);
			if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_FOCUS) {
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			updateUrl(url);
			if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			}
		}else if(playerStatus==STATUS_LOADING || playerStatus==STATUS_SWITCHING){
			showToast("player is loading, wait");
		}else if(playerStatus==STATUS_PLAYING){
			if(updateUrl(url)) {
				playerStatus = STATUS_SWITCHING;
				bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
				bus.post(new UiEvent(UiEvent.UI_ACTION.STATION_SELECTED, playerStatus, playlistManager.getSelectedUrl()));
				getPlayer().stop();
			}
		}
	}


	public Subscription getInternetListener(String url){
		String resUrl = url.substring(url.indexOf("//")+2);
		if(resUrl.contains(":")) resUrl=resUrl.substring(0,resUrl.indexOf(":"));
		else if(resUrl.contains("/")) resUrl=resUrl.substring(0,resUrl.indexOf("/"));
		Log.d("getInternetListener", resUrl);
		return ReactiveNetwork.observeInternetConnectivity(5000, resUrl,8000, 60000)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(aBoolean -> {if(aBoolean) {
					if(playerStatus==STATUS_WAITING_CONNECTIVITY){
						playerStatus=STATUS_WAITING_CONNECTIVITY;
						if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
						else {
							playerStatus=STATUS_STOPPED;
							bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,playerStatus,playlistManager.getSelectedUrl()));
						}
					}
				}});
	}
	public void listenConnectivity(String url){
		if(internetListener==null || internetListener.isUnsubscribed()) {
			Log.w("Connectivity","start listening");
			internetListener=getInternetListener(url);
		}else Log.w("Connectivity","continue listening");

	}
	public void listenConnectivity(){
		if(internetListener==null || internetListener.isUnsubscribed()) {
			Log.w("Connectivity","start listening");
			internetListener=getInternetListener("google.com");
		}else Log.w("Connectivity","continue listening");

	}
	public void dontlistenConnectivity(){
		Log.i(TAG,"dontlistenConnectivity");

		if(internetListener!=null && !internetListener.isUnsubscribed()) {
			Log.w("Connectivity","stop listening");
			internetListener.unsubscribe();
		}else Log.w("Connectivity","was not listening");
	}
	public boolean islisteningConnectivity(){
		return internetListener != null && !internetListener.isUnsubscribed();
	}
	public void abandonFocus(){
		if(playerStatus.focusStatus==FOCUS_STATUS.HAS_FOCUS
				||playerStatus.focusStatus==FOCUS_STATUS.WAITING_FOR_FOCUS) {
			audioManager.abandonAudioFocus(audioFocusChangeListener);
		}
	}







	private boolean requestFocus() {
			int r = audioManager.requestAudioFocus(
					getAudioFocusChangeListener(),
					AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
		return (r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
	}
	private boolean updateUrl(String url){
		if(playlistManager.getSelectedUrl().contains(url)) return false;
		else {
			playlistManager.setSelectedUrl(url);
			bus.post(new UiEvent(UiEvent.UI_ACTION.STATION_SELECTED, playerStatus, url));
			return true;
		}
	}


	@Override
	public void playerException(Throwable throwable) {
		Log.w(TAG, "callback playerException: "+ throwable.getMessage());
	}
	@Override
	public void playerStarted() {
		Log.w(TAG, "callback playerStarted "+ playerStatus);
		if(playerStatus==STATUS_WAITING_CONNECTIVITY) dontlistenConnectivity();
		playerStatus=STATUS_PLAYING;
		bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STARTED, playerStatus, playlistManager.getSelectedUrl()));
	}


	@Override
	public void playerStopped(int i) {
		Log.w(TAG, "callback playerStopped "+ i+"% "+ playerStatus);
		if(playerStatus==STATUS_SWITCHING) tryToPlayAsync(playlistManager.getSelectedUrl());
		else if(playerStatus==STATUS_PLAYING){
			playerStatus=STATUS_WAITING_CONNECTIVITY;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
			abandonFocus();
			listenConnectivity(playlistManager.getSelectedUrl());
		} else if(playerStatus==STATUS_WAITING_FOCUS){

		} else if(playerStatus==STATUS_WAITING_CONNECTIVITY){

		} else {
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
			abandonFocus();
		}
	}


	@Override
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs) {
		int progress=0;
		if (audioBufferCapacityMs != 0) progress=(audioBufferSizeMs * 100) / audioBufferCapacityMs;
		bus.post(new UiEvent(UiEvent.UI_ACTION.BUFFER_UPDATED,playerStatus,playlistManager.getSelectedUrl(),progress));
	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {
	}

	@Override
	public void playerMetadata(String s1, String s2) {
		if(s1!=null && s1.equals("StreamTitle")) {
			bus.post(new UiEvent(UiEvent.UI_ACTION.METADATA_UPDATED,
					playerStatus,
					playlistManager.getSelectedUrl(),
					PlaylistManager.getArtistFromString(s2),
					PlaylistManager.getTrackFromString(s2)));
		}
	}
	private void tryToPlayAsync(String url)  {
		//Assuming player is not locked, playing and device is in correct state
		if (checkSuffix(url)) {
			decodeStremLink(url);
			return;
		}
		getPlayer().playAsync(url);
	}
	public boolean checkSuffix(String streamUrl) {
		return streamUrl.contains(SUFFIX_PLS) ||
				streamUrl.contains(SUFFIX_RAM) ||
				streamUrl.contains(SUFFIX_WAX);
	}

	private void decodeStremLink(String streamLink) {
		new StreamLinkDecoder(streamLink) {
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
				tryToPlayAsync(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		}

		return mRadioPlayer;
	}
	public String getNextUrl(String current, PlaylistManager playlistManager) {
		String newUrl = current;
		if (playlistManager.isShuffle()){
			newUrl = playlistManager
					.getStations()
					.toArray()[
					new Random().nextInt(playlistManager.getStations().size())
					].toString();
		}else {
			ArrayList<String> stations = new ArrayList<String>(playlistManager.getStations());
			if (stations.indexOf(current) > stations.size() - 2) newUrl = stations.get(0);
			else newUrl = stations.get(stations.indexOf(current) + 1);
		}
		Log.i("getNextUrl","isShuffle=" + playlistManager.isShuffle());
		Log.i("getNextUrl",current+"->"+newUrl);
		return newUrl;
	}

	public String getPrevUrl(String current, PlaylistManager playlistManager) {
		String newUrl=current;
		ArrayList<String>urls=playlistManager.getStations();

		for(int i=0;i<urls.size();i++){
			if(urls.get(i).contains(current)) {
				if(i>0) newUrl=urls.get(i-1);
				else newUrl=current;
				break;
			}
		}
		Log.i("getPrevUrl",current+"->"+newUrl);
		return newUrl;
	}

	private void setSleepTimer(int seconds){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		sleepTimerTask=null;
		sleepTimerTask=new SleepTimerTask();
		sleepTimerTask.execute(seconds);
		bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.UPDATE, seconds));
		Log.d(TAG, "sleep timer created");
	}
	private void cancelSleepTimer(){
		if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
		Log.d(TAG, "sleep timer cancelled");
	}
	private class SleepTimerTask extends AsyncTask<Integer, Integer, Void> {
		@Override
		protected void onProgressUpdate(Integer... values) {
			int secondsLeft = values[0];
			Log.d(TAG, "onProgressUpdate sleep timer left "+ secondsLeft +" seconds");
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.UPDATE, secondsLeft));
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Log.d(TAG, "sleep timer finished");
			if(playerStatus!=STATUS_STOPPED) {
				playerStatus=STATUS_STOPPED;
				abandonFocus();
				getPlayer().stop();
			}
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.FINISH, -1));
		}

		@Override
		protected void onCancelled(Void aVoid) {
			cancelSleepTimer();
			bus.post(new SleepEvent(SleepEvent.SLEEP_ACTION.CANCEL, -1));
			Log.d(TAG, "sleep timer cancelled");
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



	public boolean isSleepTimerRunning(){
		return(sleepTimerTask!=null && !sleepTimerTask.isCancelled());
	}

	public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener(){
		if(audioManager==null) 	audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(audioFocusChangeListener==null) {
			audioFocusChangeListener = focusChange -> {
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS
						|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
					Log.d(TAG, "focusChange == AudioManager.AUDIOFOCUS_LOSS)");
					if(playerStatus==STATUS_PLAYING){
						playerStatus=STATUS_WAITING_FOCUS;
						getPlayer().stop();
					}
				}else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
					Log.d(TAG, "focusChange AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
					if(playerStatus==STATUS_PLAYING){
						playerStatus=STATUS_WAITING_UNMUTE;
						audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);

					}
				}else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
					Log.d(TAG, "focusChange AUDIOFOCUS_GAIN");
					if(playerStatus==STATUS_WAITING_UNMUTE) {
						playerStatus=STATUS_PLAYING;
						audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);

					}else if(playerStatus==STATUS_WAITING_FOCUS){
						playerStatus=STATUS_LOADING;
						bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED,playerStatus, playlistManager.getSelectedUrl()));
						if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
						else {
							playerStatus=STATUS_STOPPED;
							bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,playerStatus, playlistManager.getSelectedUrl()));
						}
					}
				}
			};
		}
		return audioFocusChangeListener;
	}
	public void registerMediaButtonsReciever(){
		Log.d(TAG, "registerMediaButtonsReciever");
		if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		mMediaButtonReceiverComponent = new ComponentName(getApplicationContext().getPackageName(), MediaButtonsReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);
	}

	public void unRegisterMediaButtonsReciever(){
		Log.d(TAG, "unRegisterMediaButtonsReciever");
		if(audioManager==null) 		audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(mMediaButtonReceiverComponent!=null) audioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverComponent);
	}

	public void registerHeadsetPlugReceiver(){
		Log.d(TAG, "register HeadsetPlugReceiver");
		if(headsetPlugReceiver==null) 		headsetPlugReceiver = new HeadsetPlugReceiver();
		headsetPlugIntent=getApplicationContext().registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}

	public void unRegisterHeadsetPlugReceiver(){
		Log.d(TAG, "UNregister HeadsetPlugReceiver");

		if(headsetPlugIntent!=null && headsetPlugReceiver!=null) getApplicationContext().unregisterReceiver(headsetPlugReceiver);
		headsetPlugReceiver=null;
	}

	public class HeadsetPlugReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", -1);
				switch (state) {
					case 0:
						// headset unplugged
						if(playerStatus!=STATUS_STOPPED)playPausePressed();
						break;
					case 1:
						if(playerStatus==STATUS_STOPPED){
							playerStatus=STATUS_LOADING;
							bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, playlistManager.getSelectedUrl()));
							if(requestFocus()) tryToPlayAsync(playlistManager.getSelectedUrl());
							else {
								playerStatus=STATUS_STOPPED;
								bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, playlistManager.getSelectedUrl()));
							}
						}
						break;
				}
			}
		}
	}


	public void showToast(String text){
		Observable.just(1)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(integer -> {
					if(toast!=null){
						toast.cancel();
						toast=null;
					}
					toast=Toast.makeText(getApplicationContext(), text,Toast.LENGTH_SHORT);
					toast.show();
				});
	}
}


