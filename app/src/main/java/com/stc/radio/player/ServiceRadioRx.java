package com.stc.radio.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.SleepEvent;
import com.stc.radio.player.utils.MediaButtonsReceiver;
import com.stc.radio.player.utils.Metadata;
import com.stc.radio.player.utils.RequestControlsInteraction;
import com.stc.radio.player.utils.StreamLinkDecoder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.MainActivity.saveArtBitmap;
import static com.stc.radio.player.db.NowPlaying.STATUS_IDLE;
import static com.stc.radio.player.db.NowPlaying.STATUS_PAUSING;
import static com.stc.radio.player.db.NowPlaying.STATUS_PLAYING;
import static com.stc.radio.player.db.NowPlaying.STATUS_STARTING;
import static com.stc.radio.player.db.NowPlaying.STATUS_SWITCHING;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_CONNECTIVITY;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_FOCUS;
import static com.stc.radio.player.db.NowPlaying.STATUS_WAITING_UNMUTE;
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
	public static final String EXTRA_WHICH = "com.stc.radio.player.EXTRA_WHICH";
	public static final String EXTRA_PLAY_PAUSE_PRESSED = "com.stc.radio.player.EXTRA_PLAY_PAUSE_PRESSED";
	public static final String EXTRA_NEXT_PRESSED = "com.stc.radio.player.EXTRA_NEXT_PRESSED";
	public static final String EXTRA_PREV_PRESSED = "com.stc.radio.player.EXTRA_PREV_PRESSED";
	public static final String EXTRA_PLAY_LIST_STATION_PRESSED = "com.stc.radio.player.EXTRA_PLAY_LIST_STATION_PRESSED";
	public static final String EXTRA_URL = "com.stc.radio.player.EXTRA_URL";

	public static final int EXTRA_WHICH_PREVIOUS = -1;
	public static final int EXTRA_WHICH_PLAY_PAUSE= 0;
	public static final int EXTRA_WHICH_NEXT= 1;


	private Subscription loadingFailedListener;


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
	private MultiPlayer mRadioPlayer;
	private NowPlaying nowPlaying;
	private Subscription checkDbSubscription;



	public class LocalBinder extends Binder {
		public ServiceRadioRx getService() {
			return ServiceRadioRx.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		checkDbContent();
		return mLocalBinder;
	}
	public void checkDbContent(){
		checkDbSubscription = Observable.just(1)

				.doOnNext(i -> {
					showToast("Loading playlists");
					Timber.w("Loading playlists");
				})
				.flatMap(new Func1<Integer, Observable<Integer>>() {
					@Override
					public Observable<Integer> call(Integer integer) {
						return DbHelper.observeCheckDbContent()
								.observeOn(AndroidSchedulers.mainThread())//interaction with UI must be performed on main thread
								.doOnError(new Action1<Throwable>() {//handle error before it will be suppressed
									@Override
									public void call(Throwable throwable) {
										showToast("Loading error " + throwable.getMessage());
										Timber.e(throwable, "Loading error.Check network connection");
									}
								})
								.onErrorResumeNext(Observable.empty());//prevent observable from breaking
					}
				})
				.subscribe(new Action1<Integer>() {
					@Override
					public void call(Integer integer) {
						if(checkDbSubscription!=null && !checkDbSubscription.isUnsubscribed()) checkDbSubscription.unsubscribe();
						showToast("Loading success " + integer);
						Timber.w("Loading success %d" , integer);
						isServiceConnected=true;
						if(!bus.isRegistered(getService())) bus.register(getService());
						if(nowPlaying==null){
							nowPlaying=new NowPlaying();
						}
						From from= new Select().from(Station.class);
						assertTrue(from.exists());
						nowPlaying.withMetadata(null).withShuffle(false).withStatus(0).setStation(from.executeSingle());
						for(Station s: nowPlaying.getActiveList()){
							if(s.artPath==null || !new File(s.artPath).exists()) {
								s.artPath=MainActivity.getArtPath(getService(), MainActivity.getArtName(s.url));
								s.save();
								Target target = new Target() {
									@Override
									public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
										Timber.d("From %s", from.toString());

										saveArtBitmap(s, bitmap);
									}

									@Override
									public void onBitmapFailed(Drawable errorDrawable) {
										Timber.e("From %s", errorDrawable.toString());
										Bitmap bitmap= MainActivity.drawableToBitmap(errorDrawable);

										saveArtBitmap(s, bitmap);
									}

									@Override
									public void onPrepareLoad(Drawable placeHolderDrawable) {
									}
								};
								Picasso.with(getService()).load(s.artPath).error(getDrawable(R.drawable.default_art)).into(target);

							}
						}
						bus.post(nowPlaying.getPlaylist());
						if (audioManager == null) audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
						notificationProviderRx =new NotificationProviderRx(getService(), getService());
						registerHeadsetPlugReceiver();
						registerMediaButtonsReciever();
						if(!bus.isRegistered(notificationProviderRx)) bus.register(notificationProviderRx);
						Timber.w("nowPlaying should be posted");

					}
				});
	}

	private ServiceRadioRx getService() {
		return this;
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
		if(nowPlaying.getStatus()!=STATUS_IDLE) {
			if(nowPlaying.getStatus()!=STATUS_WAITING_CONNECTIVITY) abandonFocus();
			else dontlistenConnectivity();
			nowPlaying.setStatus(STATUS_IDLE);
			getPlayer().stop();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		isServiceConnected=false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		isServiceConnected=false;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent!=null && intent.getAction()!=null){
			String action = intent.getAction();
			if(action!=null && action.contains(INTENT_USER_ACTION)){
				int which = intent.getIntExtra(EXTRA_WHICH, 0);
				Timber.w("INTENT_USER_ACTION: %d", which);
				onControlsInteraction(new RequestControlsInteraction(which));

			}else if(action!=null && action.contains(INTENT_SET_BUFFER_SIZE)) {
				//DbHelper.setBufferSize(intent.getIntExtra(EXTRA_AUDIO_BUFFER_CAPACITY, 800));
				//DbHelper.setDecodeSize(intent.getIntExtra(EXTRA_AUDIO_DECODE_CAPACITY, 400));
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

	@Subscribe()
	public void onNewStationToPlay(Station station){
		assertTrue(station!=null);
		Timber.w("onNewItemToPlay item %s", station.name);
		if(nowPlaying.getStation().equals(station)){
			onPlayPause();
		}else {
			switch (nowPlaying.getStatus()){
				case STATUS_IDLE:
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					tryToPlayAsync(nowPlaying.getStation().url);
					break;
				case STATUS_PLAYING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					getPlayer().stop();
					break;
				case STATUS_PAUSING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					break;
				case STATUS_STARTING:
				case STATUS_SWITCHING:
					nowPlaying.withStation(station).setStatus(STATUS_SWITCHING);
					getPlayer().stop();
					//TODO wait 1 sec and play
					break;
				case STATUS_WAITING_UNMUTE:
					showToast("STATUS_WAITING_UNMUTE");
					Timber.e("STATUS_WAITING_UNMUTE");
					break;
				case STATUS_WAITING_CONNECTIVITY:
					dontlistenConnectivity();
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					tryToPlayAsync(nowPlaying.getStation().url);
					break;
				case STATUS_WAITING_FOCUS:
					abandonFocus();
					nowPlaying.withStation(station).setStatus(STATUS_STARTING);
					tryToPlayAsync(nowPlaying.getStation().url);
					break;

			}
		}
	}
	public void onPlayPause() {
		Timber.w("onPlayPause");
		switch(nowPlaying.getStatus()){
			case STATUS_IDLE:
				if(nowPlaying.getStation()==null) {
					showToast("No station selected");
					Timber.e("No station selected");
				}else {
					nowPlaying.setStatus(STATUS_STARTING);
					tryToPlayAsync(nowPlaying.getStation().url);
				}
				break;
			case STATUS_PLAYING:
				nowPlaying.setStatus(STATUS_PAUSING);
				getPlayer().stop();
				break;
			case STATUS_PAUSING:
				break;
			case STATUS_STARTING:
			case STATUS_SWITCHING:
				nowPlaying.setStatus(STATUS_IDLE);
				getPlayer().stop();
				break;
			case STATUS_WAITING_UNMUTE:
				showToast("STATUS_WAITING_UNMUTE");
				Timber.e("STATUS_WAITING_UNMUTE");
				break;
			case STATUS_WAITING_CONNECTIVITY:
				nowPlaying.setStatus(STATUS_IDLE);
				dontlistenConnectivity();
				break;
			case STATUS_WAITING_FOCUS:
				nowPlaying.setStatus(STATUS_IDLE);
				abandonFocus();
				break;
			default:
				showToast("onPlayPause");
				Timber.e("onPlayPause");
		}
	}



	private void resetPlayer() {
		showToast("resetPlayer");
		Timber.e("resetPlayer %d", nowPlaying.getStatus());
		if(nowPlaying.getStatus()!=STATUS_IDLE){
			abandonFocus();
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
			getPlayer().stop();
			nowPlaying.setStatus(STATUS_IDLE);
		}
	}

	@Subscribe()
	private void onControlsInteraction(RequestControlsInteraction request) {
		if(request.which()==0) onPlayPause();
		else {
			List<Station> list = nowPlaying.getActiveList();
			int newPos = 0;
			if (list == null || list.size() <= 0) {
				showToast("null list");
				Timber.e("null list, request was: %d", request.which());
			} else {
				int pos = nowPlaying.getStation().position;
				if (pos < 0) {
					showToast("null pos");
					Timber.e("null pos, request was: %d", request.which());
					return;
				}
				if (request.which() > 0) {
					if (nowPlaying.getShuffle()) newPos = new Random().nextInt(list.size() - 1);
					else newPos = nowPlaying.getStation().position;
					if (nowPlaying.getStation().position != list.size() - 1) newPos += 1;
				} else {
					newPos = nowPlaying.getStation().position;
					if (nowPlaying.getStation().position != 0) newPos -= 1;
				}
				for (Station s : list)
					if (s.position == newPos) {
						bus.post(s);
						return;
					}
				showToast("newPos notFound in list");
				Timber.e(" newPos: %d", newPos);
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
					if(nowPlaying.getStatus()==STATUS_WAITING_CONNECTIVITY){
						nowPlaying.setStatus(STATUS_STARTING);
						if(requestFocus()) tryToPlayAsync(nowPlaying.getStation().url);
						else nowPlaying.setStatus(STATUS_IDLE);
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
		if(nowPlaying.getStatus()!=STATUS_IDLE) {
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
		Timber.w("TEST callback playerException: "+ throwable.getMessage());
	}
	@Override
	public void playerStarted() {
		Timber.w("callback playerStarted when status was %d", nowPlaying.getStatus());
		if(nowPlaying.getStatus()==STATUS_WAITING_CONNECTIVITY) dontlistenConnectivity();
		if(loadingFailedListener != null && !loadingFailedListener.isUnsubscribed()) loadingFailedListener.unsubscribe();
		nowPlaying.setStatus(STATUS_PLAYING);
	}


	@Override
	public void playerStopped(int i) {
		Timber.w("TEST callback playerStopped %d%", i);
		if(nowPlaying.getStatus()==STATUS_SWITCHING) tryToPlayAsync(nowPlaying.getStation().url);
		else if(nowPlaying.getStatus()==STATUS_PLAYING){
			nowPlaying.setStatus(STATUS_WAITING_CONNECTIVITY);
			abandonFocus();
			listenConnectivity(nowPlaying.getStation().url);
		} else if(nowPlaying.getStatus()!=STATUS_WAITING_FOCUS &&
				nowPlaying.getStatus()!=STATUS_WAITING_CONNECTIVITY){
			nowPlaying.setStatus(STATUS_IDLE);
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
	public void playerPCMFeedBuffer(boolean intermediate, int audioBufferSizeMs, int audioBufferCapacityMs){
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
			nowPlaying.setMetadata(new Metadata(a, s));
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
			mRadioPlayer = new MultiPlayer(this, 2000, 1000);
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
			if(nowPlaying.getStatus()!=STATUS_IDLE){
				nowPlaying.setStatus(STATUS_IDLE);
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
					if(nowPlaying.getStatus()==STATUS_PLAYING){
						nowPlaying.setStatus(STATUS_WAITING_FOCUS);
						getPlayer().stop();
					}
				}else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
					Log.d(TAG, "focusChange AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
					if(nowPlaying.getStatus()==STATUS_PLAYING){
						nowPlaying.setStatus(STATUS_WAITING_UNMUTE);
						audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
					}
				}else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
					Log.d(TAG, "focusChange AUDIOFOCUS_GAIN");
					if(nowPlaying.getStatus()==STATUS_WAITING_UNMUTE) {
						nowPlaying.setStatus(STATUS_PLAYING);
						audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
					}else if(nowPlaying.getStatus()==STATUS_WAITING_FOCUS){
						nowPlaying.setStatus(STATUS_STARTING);
						if(requestFocus()) tryToPlayAsync(nowPlaying.getStation().url);
						else {
							nowPlaying.setStatus(STATUS_IDLE);
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
							if(nowPlaying.getStatus()!=STATUS_IDLE) onPlayPause();
							break;
						case 1:
							if (nowPlaying.getStatus()==STATUS_IDLE) {

								nowPlaying.setStatus(STATUS_STARTING);
								if (requestFocus())	tryToPlayAsync(nowPlaying.getStation().url);
								else nowPlaying.setStatus(STATUS_IDLE);

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


/*

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
*/



}


