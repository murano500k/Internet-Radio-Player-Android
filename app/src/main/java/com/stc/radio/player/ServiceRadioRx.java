package com.stc.radio.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.MainActivity;
import com.stc.radio.player.ui.Metadata;
import com.stc.radio.player.ui.NotificationProviderRx;
import com.stc.radio.player.ui.SleepEvent;
import com.stc.radio.player.ui.StationListItem;
import com.stc.radio.player.ui.UiEvent;
import com.stc.radio.player.utils.PabloPicasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.activeandroid.Cache.getContext;
import static junit.framework.Assert.assertTrue;


public class ServiceRadioRx extends Service implements PlayerCallback{
	public static final String INTENT_RESET = "com.stc.radio.player.INTENT_RESET";
	public static final String INTENT_SLEEP_SET = "com.stc.radio.player.INTENT_SLEEP_SET";
	public static final String INTENT_SLEEP_CANCEL = "com.stc.radio.player.INTENT_SLEEP_CANCEL";
	public static final String INTENT_SET_BUFFER_SIZE = "com.stc.radio.player.INTENT_SET_BUFFER_SIZE";
	public static final String EXTRA_AUDIO_BUFFER_CAPACITY = "com.android.murano500k" +
			".EXTRA_AUDIO_BUFFER_CAPACITY";
	public static final String EXTRA_AUDIO_DECODE_CAPACITY = "com.android.murano500k" +
			".EXTRA_AUDIO_DECODE_CAPACITY";
	public static final String EXTRA_SLEEP_SECONDS= "com.stc.radio.player.INTENT_SLEEP_CANCEL";

	public static final String INTENT_USER_ACTION = "com.stc.radio.player.INTENT_USER_ACTION";
	public static final String EXTRA_PLAY_PAUSE_PRESSED = "com.stc.radio.player.EXTRA_PLAY_PAUSE_PRESSED";
	public static final String EXTRA_NEXT_PRESSED = "com.stc.radio.player.EXTRA_NEXT_PRESSED";
	public static final String EXTRA_PREV_PRESSED = "com.stc.radio.player.EXTRA_PREV_PRESSED";
	public static final String EXTRA_PLAY_LIST_STATION_PRESSED = "com.stc.radio.player.EXTRA_PLAY_LIST_STATION_PRESSED";
	public static final String EXTRA_URL = "com.stc.radio.player.EXTRA_URL";

	public static final PlayerStatus STATUS_PLAYING= new PlayerStatus("STATUS_PLAYING", 1, LOADING_TYPE.NOT_LOADING, FOCUS_STATUS.HAS_FOCUS);
	public static final PlayerStatus STATUS_STOPPED= new PlayerStatus("STATUS_STOPPED", -1, LOADING_TYPE.NOT_LOADING, FOCUS_STATUS.FOCUS_NOT_REQUESTED);
	public static final PlayerStatus STATUS_WAITING_CONNECTIVITY= new PlayerStatus("STATUS_WAITING_CONNECTIVITY", 0, LOADING_TYPE.WAITING_CONNECTIVYTY, FOCUS_STATUS.FOCUS_NOT_REQUESTED);
	public static final PlayerStatus STATUS_WAITING_FOCUS= new PlayerStatus("STATUS_WAITING_FOCUS", 0, LOADING_TYPE.WAITING_FOCUS, FOCUS_STATUS.WAITING_FOR_FOCUS);
	public static final PlayerStatus STATUS_WAITING_UNMUTE= new PlayerStatus("STATUS_WAITING_UNMUTE", 0, LOADING_TYPE.WAITING_UNMUTE, FOCUS_STATUS.MUTED);
	public static final PlayerStatus STATUS_LOADING= new PlayerStatus("STATUS_LOADING", 0, LOADING_TYPE.LOADING_STREAM, FOCUS_STATUS.HAS_FOCUS);
	public static final PlayerStatus STATUS_SWITCHING= new PlayerStatus("STATUS_SWITCHING", 1, LOADING_TYPE.SWITCHING_STREAM, FOCUS_STATUS.HAS_FOCUS);
	private Bitmap currentArt;
	private Subscription loadingFailedListener;

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
	private MultiPlayer mRadioPlayer;
	private PlayerStatus playerStatus;
	private String currentUrl;
	private List<StationListItem> listStations;

	public String getCurrentUrl() {
		if(currentUrl!=null) return currentUrl;
		Timber.w("currentUrl is null");
		return "";
	}

	public String updateCurrentUrl(String currentUrl) {
		if(this.currentUrl!=null && !this.currentUrl.contains(currentUrl)) currentArt=null;
		String lastUrl="";
		if(this.currentUrl!=null) lastUrl=this.currentUrl;
		this.currentUrl = currentUrl;
		Timber.v("%s -> %s",lastUrl, currentUrl);
		//if(!lastUrl.contains(currentUrl)) bus.post(new UiEvent(UiEvent.UI_ACTION.STATION_SELECTED, playerStatus, currentUrl));
		return lastUrl;
	}

	public boolean isPlaying() {
		return playerStatus==STATUS_PLAYING;
	}
	public PlayerStatus getPlayerStatus() {
		return playerStatus;
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
		playerStatus=STATUS_STOPPED;
		if(!bus.isRegistered(notificationProviderRx)) bus.register(notificationProviderRx);
		if(!bus.isRegistered(this)) bus.register(this);
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent!=null && intent.getAction()!=null){
			String action = intent.getAction();
			if(action!=null && action.contains(INTENT_USER_ACTION)){
				Timber.w("intent: %s %s %s %s", intent.getExtras().getString(EXTRA_PLAY_PAUSE_PRESSED, "")
						,intent.getExtras().getString(EXTRA_NEXT_PRESSED, "")
						,intent.getExtras().getString(EXTRA_PREV_PRESSED, "")
						,intent.getExtras().getString(EXTRA_PLAY_LIST_STATION_PRESSED, ""));

				if(intent.getExtras().containsKey(EXTRA_PLAY_PAUSE_PRESSED))
					playPausePressed();
				else if (intent.getExtras().containsKey(EXTRA_NEXT_PRESSED))
					playPrevNextPressed(1);
				else if (intent.getExtras().containsKey(EXTRA_PREV_PRESSED))
					playPrevNextPressed(-1);
				else if (intent.getExtras().containsKey(EXTRA_PLAY_LIST_STATION_PRESSED))
					playUrlPressed(intent.getStringExtra(EXTRA_URL));

			}else if(action!=null && action.contains(INTENT_SET_BUFFER_SIZE)) {
				DbHelper.setBufferSize(intent.getIntExtra(EXTRA_AUDIO_BUFFER_CAPACITY, 800));
				DbHelper.setDecodeSize(intent.getIntExtra(EXTRA_AUDIO_DECODE_CAPACITY, 400));
				if(playerStatus!=STATUS_STOPPED) {
					playerStatus=STATUS_LOADING;
					bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
					getPlayer().stop();
				}
			}else if(action!=null && action.contains(INTENT_RESET)){
				resetPlayer();
			} else if (action!=null && action.equals(INTENT_SLEEP_SET)) {
				Log.d(TAG, "serv INTENT_SLEEP_SET");
				int secs = intent.getIntExtra(EXTRA_SLEEP_SECONDS, -254);
				if(secs==-254)             Log.d(TAG, "secs ERROR");
				Log.d(TAG, "secs" + secs);
				setSleepTimer(secs);
			}else if (action!=null && action.equals(INTENT_SLEEP_CANCEL)) {
				Log.d(TAG, "serv CANCEL");
				cancelSleepTimer();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void resetPlayer() {
		bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
		playerStatus=STATUS_STOPPED;
		abandonFocus();
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		getPlayer().stop();
		bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
	}


	private void playPrevNextPressed(int or) {
		Timber.v("play %s", or>0 ? "Next":"Prev");
		List<Station> list=DbHelper.getPlaylistStations(DbHelper.getNowPlaying().activePlaylistId);
		int oldPos = DbHelper.getCurrentPosition();
		if(or>0) {
			if (DbHelper.isShuffle()) {
				playUrlPressed(list.get(new Random().nextInt(list.size() - 1)).url);
			} else {
				Iterator<Station> iterator = list.iterator();
				while (iterator.hasNext()) {
					Station station = iterator.next();
					if (station.position == oldPos) {
						playUrlPressed(iterator.hasNext() ? iterator.next().url : station.url);
						break;
					}
				}
			}
		}else {



		}
	}


	private void playPausePressed() {
		Log.w("playPausePressed", ""+playerStatus);
		if(playerStatus==STATUS_STOPPED){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			if(requestFocus()) tryToPlayAsync(getCurrentUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_CONNECTIVITY) {
			playerStatus=STATUS_STOPPED;
			dontlistenConnectivity();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
		}
		else if(playerStatus==STATUS_WAITING_UNMUTE) {
			Log.e(TAG, "STATUS_WAITING_UNMUTE playPausePressed");
			showToast("STATUS_WAITING_UNMUTE");
		}
		else if(playerStatus==STATUS_SWITCHING) {
			Log.e(TAG, "STATUS_SWITCHING playPausePressed");
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			getPlayer().stop();
		}
		else if(playerStatus==STATUS_WAITING_FOCUS) {
			playerStatus=STATUS_STOPPED;
			abandonFocus();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
		} else if(playerStatus==STATUS_LOADING){
			Log.e(TAG, "STATUS_LOADING playPausePressed");
			playerStatus=STATUS_STOPPED;
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			getPlayer().stop();

		} else if(playerStatus==STATUS_PLAYING){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			getPlayer().stop();
		}
	}

	private void playUrlPressed(String url) {
		Log.w("playUrlPressed", ""+playerStatus);
		Timber.w("url %s", url);
		if(playerStatus==STATUS_WAITING_UNMUTE) {
			Log.e(TAG, "STATUS_WAITING_UNMUTE playPausePressed");
			showToast("STATUS_WAITING_UNMUTE");
		} if(playerStatus==STATUS_STOPPED){
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			updateCurrentUrl(url);
			updateArt();
			if(requestFocus()) tryToPlayAsync(getCurrentUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,
						playerStatus, getCurrentUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_CONNECTIVITY) {
			playerStatus=STATUS_STOPPED;
			dontlistenConnectivity();
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			updateCurrentUrl(url);
			updateArt();
			if(requestFocus()) tryToPlayAsync(getCurrentUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			}
		}
		else if(playerStatus==STATUS_WAITING_FOCUS) {
			playerStatus=STATUS_LOADING;
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			updateCurrentUrl(url);
			updateArt();
			if(requestFocus()) tryToPlayAsync(getCurrentUrl());
			else {
				playerStatus=STATUS_STOPPED;
				bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			}
		}else if(playerStatus==STATUS_LOADING || playerStatus==STATUS_SWITCHING){
			showToast("player is loading, wait");
		}else if(playerStatus==STATUS_PLAYING){
			if(!updateCurrentUrl(url).contains(url)) {
				playerStatus = STATUS_SWITCHING;
				bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
				updateArt();
				//bus.post(new UiEvent(UiEvent.UI_ACTION.STATION_SELECTED, playerStatus, getCurrentUrl()));
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
						if(requestFocus()) tryToPlayAsync(getCurrentUrl());
						else {
							playerStatus=STATUS_STOPPED;
							bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,playerStatus,getCurrentUrl()));
						}
					}
				}});
	}
	public Subscription getLoadingFailedListener(String url){
		String resUrl = url.substring(url.indexOf("//")+2);
		if(resUrl.contains(":")) resUrl=resUrl.substring(0,resUrl.indexOf(":"));
		else if(resUrl.contains("/")) resUrl=resUrl.substring(0,resUrl.indexOf("/"));
		Log.d("getInternetListener", resUrl);
		return ReactiveNetwork.observeInternetConnectivity(1000, resUrl,8000, 5000)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(aBoolean -> {
					if(!aBoolean) {
						showToast("Connection failed");
						resetPlayer();
						if(loadingFailedListener!=null && !loadingFailedListener.isUnsubscribed()) {
							loadingFailedListener.unsubscribe();
						}
					}
				});
	}
	public void listenConnectivity(String url){
		if(internetListener==null || internetListener.isUnsubscribed()) {
			Log.w("Connectivity","start listening");
			internetListener=getInternetListener(url);
		}else Log.w("Connectivity","continue listening");

	}
	public void dontlistenConnectivity(){
		Log.i(TAG,"dontlistenConnectivity");

		if(internetListener!=null && !internetListener.isUnsubscribed()) {
			Log.w("Connectivity","stop listening");
			internetListener.unsubscribe();
		}else Log.w("Connectivity","was not listening");
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



	@Override
	public void playerException(Throwable throwable) {
		Log.w(TAG, "callback playerException: "+ throwable.getMessage());
	}
	@Override
	public void playerStarted() {
		Log.w(TAG, "callback playerStarted "+ playerStatus);
		if(playerStatus==STATUS_WAITING_CONNECTIVITY) dontlistenConnectivity();
		playerStatus=STATUS_PLAYING;
		DbHelper.setPlayerState(MainActivity.UI_STATE.PLAYING);
		bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STARTED, playerStatus, getCurrentUrl()));
		if(loadingFailedListener != null && !loadingFailedListener.isUnsubscribed()) loadingFailedListener.unsubscribe();
	}


	@Override
	public void playerStopped(int i) {
		Log.w(TAG, "callback playerStopped "+ i+"% "+ playerStatus);
		if(playerStatus==STATUS_SWITCHING) tryToPlayAsync(getCurrentUrl());
		else if(playerStatus==STATUS_PLAYING){
			playerStatus=STATUS_WAITING_CONNECTIVITY;
			DbHelper.setPlayerState(MainActivity.UI_STATE.LOADING);
			bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
			abandonFocus();
			listenConnectivity(getCurrentUrl());
		} else if(playerStatus!=STATUS_WAITING_FOCUS &&
				playerStatus!=STATUS_WAITING_CONNECTIVITY){
			playerStatus=STATUS_STOPPED;
			DbHelper.setPlayerState(MainActivity.UI_STATE.IDLE);
			bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
			abandonFocus();
		}
	}
	/**
	 * This method is called periodically by PCMFeed.
	 *
	 * isPlaying false means that the PCM data are being buffered,
	 *          but the audio is not playing yet
	 *
	 *  audioBufferSizeMs the buffered audio data expressed in milliseconds of playing
	 *  audioBufferCapacityMs the total capacity of audio buffer expressed in milliseconds of playing
	 */
/*public void playerPCMFeedBuffer( final boolean isPlaying,
                                     final int audioBufferSizeMs, final int audioBufferCapacityMs ) {

        uiHandler.post( new Runnable() {
            public void run() {
                progress.setProgress( audioBufferSizeMs * progress.getMax() / audioBufferCapacityMs );
                if (isPlaying) txtStatus.setText( R.string.text_playing );
            }
        });
    }*/
	@Override
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs) {
		bus.post(new BufferUpdate(audioBufferSizeMs, audioBufferCapacityMs, intermediate));

	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {
	}

	@Override
	public void playerMetadata(String s1, String s2) {

		if(s1!=null && s1.equals("StreamTitle")) {
			String a=getArtistFromString(s2);
			String s=getTrackFromString(s2);

			//Timber.v("1artist %s",a);
			//Timber.v("2track %s",s);
			bus.post(new Metadata(a, s, getCurrentUrl()));
		}
	}

	static String getArtistFromString(String data) {
		String artistName = "";
		if (data != null && data.contains(" - ")) {
			artistName = data.substring(0, data.indexOf(" - "));
		}
		return artistName;
	}

	static String getTrackFromString(String data) {
		String trackName = "";
		if (data != null && data.contains(" - ")) {
			trackName = data.substring(data.indexOf(" - ") + 3);
		}
		return trackName;
	}
	private void tryToPlayAsync(String url)  {
		//Assuming player is not locked, playing and device is in correct state
		if (checkSuffix(url)) {
			decodeStremLink(url);
			return;
		}
		if(loadingFailedListener != null && !loadingFailedListener.isUnsubscribed()) loadingFailedListener.unsubscribe();
		loadingFailedListener=getLoadingFailedListener(url);

		getPlayer().playAsync(url);
	}
	public boolean checkSuffix(String streamUrl) {
		String SUFFIX_PLS = ".pls";
		String SUFFIX_RAM = ".ram";
		String SUFFIX_WAX = ".wax";
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
			java.net.URL.setURLStreamHandlerFactory(protocol -> {
				Log.d("LOG", "Asking for stream handler for protocol: '" + protocol + "'");
				if ("icy".equals(protocol))
					return new com.spoledge.aacdecoder.IcyURLStreamHandler();
				return null;
			});
		} catch (Throwable t) {
			Log.w("LOG", "Cannot set the ICY URLStreamHandler - maybe already set ? - " + t);
		}
		if (mRadioPlayer == null) {
			mRadioPlayer = new MultiPlayer(this, DbHelper.getBufferSize(), DbHelper.getDecodeSize());
			mRadioPlayer.setResponseCodeCheckEnabled(false);
		}

		return mRadioPlayer;
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
						bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED,playerStatus, getCurrentUrl()));
						if(requestFocus()) tryToPlayAsync(getCurrentUrl());
						else {
							playerStatus=STATUS_STOPPED;
							bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED,playerStatus, getCurrentUrl()));
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
				if (isServiceConnected) {
					int state = intent.getIntExtra("state", -1);
					switch (state) {
						case 0:
							// headset unplugged
							if (playerStatus != STATUS_STOPPED) playPausePressed();
							break;
						case 1:
							if (playerStatus == STATUS_STOPPED) {
								playerStatus = STATUS_LOADING;
								bus.post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, playerStatus, getCurrentUrl()));
								if (requestFocus())
									tryToPlayAsync(getCurrentUrl());
								else {
									playerStatus = STATUS_STOPPED;
									bus.post(new UiEvent(UiEvent.UI_ACTION.PLAYBACK_STOPPED, playerStatus, getCurrentUrl()));
								}
							}
							break;
					}
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

	@Subscribe()
	public void onArtRequested(RequestArt requestArt){
		Timber.v("onArtRequested %s",currentArt!=null ? currentArt.toString() : "null");
		if(currentArt!=null)bus.post(currentArt);
	}
	@Subscribe()
	public void onNewItemToPlay(StationListItem item){
		assertTrue(item!=null);
		Timber.v("item %s", item.station.name);
		playUrlPressed(item.station.url);
	}
	@Subscribe()
	public void onPlayPause(Integer i){
		Timber.v("onPlayPause %d", i);
		if(i==0) playPausePressed();
	}

	public void updateArt() {
		if (currentArt != null) bus.post(currentArt);
		else if(currentUrl!=null){
			String artUrl = DbHelper.getArtUrl(currentUrl);
			Timber.w("station artUrl=%s", artUrl);
			PabloPicasso.with(getContext()).load(Uri.parse(artUrl))
					.placeholder(R.drawable.ic_default_art)
					.error(android.R.drawable.stat_notify_error)
					//.resizeDimen(R.dimen.list_item_art_size, R.dimen.list_item_art_size)
					.into(new Target() {
						@Override
						public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
							bus.post(bitmap);
						}

						@Override
						public void onBitmapFailed(Drawable errorDrawable) {
							bus.post(drawableToBitmap(errorDrawable));
						}

						@Override
						public void onPrepareLoad(Drawable placeHolderDrawable) {
							//bus.post(drawableToBitmap(placeHolderDrawable));
						}
					});
		}else Timber.e("NO URL");
	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

}


