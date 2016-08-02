package com.android.murano500k.newradio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.android.murano500k.newradio.StationContent.initStations;

/**
 * The type Service radio.
 */
public class ServiceRadio extends Service implements PlayerCallback{
//TODO: Widget, notification, sleeptimer, design (theme, colors)
	//TODO:Interruptions: connectivity, phone, headphones plug, audiofocus
	private String stationName = "";
	private String singerName = "";
	private String songName = "";
	private int smallImage = R.drawable.default_art;
	private Bitmap artImage=null;

	private final int AUDIO_BUFFER_CAPACITY_MS = 800;
	private final int AUDIO_DECODE_CAPACITY_MS = 400;
	private final String SUFFIX_PLS = ".pls";
	private final String SUFFIX_RAM = ".ram";
	private final String SUFFIX_WAX = ".wax";

	private final String TAG= "ServiceRadio";
	private MultiPlayer mRadioPlayer;
	private boolean isSwitching;
	private boolean isClosedFromNotification = false;
	private boolean mLock;
	/**
	 * The M local binder.
	 */
	public final IBinder mLocalBinder = new LocalBinder();
	private InterruptHandler interruptHandler;
	private boolean isPlaying;
	private MediaSession mediaSession;
	private Subscription stationsSubscription;

	/**
	 * The enum State.
	 */
	public enum State {
		/**
		 * Idle state.
		 */
		IDLE,
		/**
		 * Playing state.
		 */
		PLAYING,
		/**
		 * Stopped state.
		 */
		STOPPED,
	}
	private State mRadioState;

	/**
	 * The Notifier.
	 */
	public NotifierRadio notifier;
	private Station currentStation;
	private NotificationManager mNotificationManager;

	private static ArrayList<Station> stations;
	private static ArrayList<Station> stationsFav;



	private boolean favOnly;


	/**
	 * Gets player.
	 *
	 * @return the player
	 */
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
	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		return mLocalBinder;
	}

	/**
	 * The type Local binder.
	 */
	public class LocalBinder extends Binder {
		/**
		 * Gets service.
		 *
		 * @return the service
		 */
		public ServiceRadio getService() {
			return ServiceRadio.this;
		}
	}

	@Override
	public void playerStarted() {
		mRadioState = State.PLAYING;
		mLock = false;
		isClosedFromNotification = false;
		notifier.notifyStationSelected(currentStation.url);

		notifier.notifyPlaybackStarted(currentStation.url);
	}

	@Override
	public void playerPCMFeedBuffer(boolean b, int i, int i1) {

	}

	@Override
	public void playerStopped(int i) {
		mRadioState = State.IDLE;
		Log.d(TAG, "playerStopped(int i)= " +i);

		if (!isClosedFromNotification) {
			//buildNotification();
			notifier.notifyPlaybackStopped(true);
		} else{
			notifier.notifyPlaybackStopped(false);
			isClosedFromNotification = false;
		}
		mLock = false;
		Log.d(TAG, "Player stopped. State : " + mRadioState);
		if (isSwitching)
			play(currentStation.url);
	}

	@Override
	public void playerException(Throwable throwable) {
		mRadioState = State.STOPPED;
		mLock = false;

		Log.d(TAG, "playerException: " + throwable.getMessage());
		interruptHandler.setInterrupted(true);
		notifier.notifyPlaybackErrorOccured();


	}

	@Override
	public void playerMetadata(String s, String s1) {
		notifier.notifyMetaDataChanged(s,s1);
	}

	@Override
	public void playerAudioTrackCreated(AudioTrack audioTrack) {

	}


	/**
	 * Instantiates a new Service radio.
	 */
	public ServiceRadio() {



	}
	@Override
	public void onCreate() {
		super.onCreate();
		mRadioState = State.IDLE;
		notifier=new NotifierRadio();
		interruptHandler = new InterruptHandler(getApplicationContext(), this);
		notifier.registerListener(interruptHandler);
		isSwitching = false;
		mLock = false;
		notifier.notifyRadioConnected();

		if(stations==null) stations = initStations();
		getPrefsFromStorage();

		getPlayer();
		initMediaSession();
		//notifier.notifyListChanged(getStations());

	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		Log.d(TAG,"onStartCommand " + action);
		Log.d(TAG, "INTENT " + action);
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent event= intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			int keycode = event.getKeyCode();
			int keyAction = event.getAction();
			switch (keycode) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "KEYCODE_MEDIA_NEXT");
						isClosedFromNotification=false;
						playNext();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "KEYCODE_MEDIA_PREVIOUS");
						isClosedFromNotification=false;
						playPrev();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY_PAUSE");
						intentActionPlayPause(intent);
					}
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PAUSE ");
						intentActionPause(intent);
					}
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					if (keyAction == KeyEvent.ACTION_DOWN) {
						Log.d(TAG, "Trigered KEYCODE_MEDIA_PLAY");
						intentActionPlay(intent);
					}
					break;
				default:
					break;
			}
		} else if (action.equals(Constants.INTENT_SLEEP_TIMER_SET)) {
			Log.d(TAG, "serv INTENT_SLEEP_TIMER_SET");
			int secs = intent.getIntExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, -254);
			if(secs==-254)             Log.d(TAG, "secs ERROR");
			Log.d(TAG, "secs" + secs);
			//setSleepTimer(secs);
		}else if (action.equals(Constants.INTENT_SLEEP_TIMER_CANCEL)) {
			Log.d(TAG, "serv INTENT_SLEEP_TIMER_CANCEL");
			//cancelSleepTimer(); //TODO uncomment sleep
		} else if (action.equals(Constants.INTENT_CLOSE_NOTIFICATION)) {
			Log.d(TAG,"serv INTENT_CLOSE_NOTIFICATION");
			isClosedFromNotification=true;
			if(isPlaying())stop();

			if(mNotificationManager != null) {
				mNotificationManager.cancel(Constants.NOTIFICATION_ID);
			}
		} else if (action.equals(Constants.INTENT_PLAY_PAUSE)) {
			intentActionPlayPause(intent);
		} else if (action.equals(Constants.INTENT_PLAY_NEXT)) {
			Log.d(TAG, "INTENT_PLAY_NEXT");
			isClosedFromNotification=false;
			playNext();
		} else if (action.equals(Constants.INTENT_PLAY_RANDOM)) {
			Log.d(TAG, "INTENT_PLAY_RANDOM");
			isClosedFromNotification=false;
			playRandom();
		} else if (action.equals(Constants.INTENT_PLAY_PREV)) {
			Log.d(TAG, "INTENT_PLAY_PREV");
			isClosedFromNotification=false;
			playPrev();
		} else if (action.equals(Constants.INTENT_PAUSE_PLAYBACK)) {
			Log.d(TAG, "INTENT_PAUSE_PLAYBACK");
			intentActionPause(intent);
		} else if (action.contains(Constants.INTENT_RESUME_PLAYBACK)) {
			intentActionPlay(intent);
		} else if (action.contains(Constants.INTENT_HANDLE_CONNECTIVITY)) {
			if (mRadioState==State.STOPPED){
				intentActionPlay(intent);
			}
		} else if (action.contains(Constants.INTENT_UPDATE_FAVORITE_STATION)) {
			int indexToUpdate;
			boolean favToUpdate;
			indexToUpdate= intent.getIntExtra(Constants.DATA_STATION_INDEX, -1);
			favToUpdate = intent.getBooleanExtra(Constants.DATA_STATION_FAVORITE, false);
			Log.d(TAG, "updating station indexToUpdate " + indexToUpdate + ", fav="+favToUpdate);

			if(indexToUpdate!=-1) for(Station station: stations) if(station.id==indexToUpdate) {
				station.fav=favToUpdate;
				Log.d(TAG, "station " + station.url + " was set fav="+favToUpdate);
			}

		} else if (action.contains(Constants.INTENT_UPDATE_STATIONS)) {
			favOnly = intent.getBooleanExtra(Constants.DATA_FAV_ONLY, isFavOnly());
			stationsSubscription = stationsObservable()
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.doOnCompleted(() -> {
						stop();
						currentStation=getStations().get(0);
						notifier.notifyListChanged(getStations());
						savePrefsToStorage();
					})
					.subscribe(stations1 -> {
						if(isFavOnly()) stationsFav=stations1;
						else stations=stations1;
						return;
					});
		}
		return START_NOT_STICKY;
	}

	public rx.Observable<ArrayList<Station>> stationsObservable() {
		return Observable.create(new Observable.OnSubscribe<ArrayList<Station>>() {
			@Override
			public void call(Subscriber<? super ArrayList<Station>> subscriber) {
				if(!isFavOnly()) subscriber.onNext(stations);
				else{
					ArrayList<Station> favStations = new ArrayList<Station>();
					for(Station station: stations){
						if(station.fav) favStations.add(station);
					}
					if(favStations.size()==0){
						Log.d(TAG,"No favorite stations. All stations selected");
						//Toast.makeText(getApplicationContext(), "No favorite stations. All stations selected", Toast.LENGTH_SHORT).show();
						subscriber.onNext(stations);
					}
					else subscriber.onNext(favStations);
				}
				subscriber.onCompleted();

			}
		});
	}

	private void intentActionPause(Intent intent) {
		Log.d(TAG, "INTENT_PAUSE_PLAYBACK");
		isClosedFromNotification=false;
		if(isPlaying())stop();
	}
	private void intentActionPlay(Intent intent) {
		Log.d(TAG, "INTENT_RESUME_PLAYBACK");
		isClosedFromNotification=false;
		String stationUrl;
		if(intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL)!=null) {
			stationUrl = intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL);
			currentStation=getStationByUrl(stationUrl);
			play(stationUrl);
		}
		else if (currentStation!=null) play(currentStation.url);
		else Toast.makeText(getApplicationContext(), "No station selected", Toast.LENGTH_SHORT).show();
	}

	private void intentActionPlayPause(Intent intent) {
		Log.d(TAG, "INTENT_PLAY_PAUSE");
		String stationUrl;
		isClosedFromNotification=false;
		if(isPlaying())stop();
		else {
			if(intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL)!=null) {
				stationUrl = intent.getStringExtra(Constants.DATA_CURRENT_STATION_URL);
				currentStation=getStationByUrl(stationUrl);
				play(stationUrl);
			}
			else if (currentStation!=null) play(currentStation.url);
			else Toast.makeText(getApplicationContext(), "No station selected", Toast.LENGTH_SHORT).show();
		}

	}
	private void playNext() {
		if(currentStation==null || currentStation.id>=getStations().size()-1) currentStation=getStations().get(0);
		currentStation=getStations().get(currentStation.id+1);
		if(currentStation!=null) play(currentStation.url);
	}
	private void playPrev() {
		if(currentStation==null || currentStation.id<=1) currentStation=getStations().get(1);
		currentStation=getStations().get(currentStation.id-1);
		if(currentStation!=null) play(currentStation.url);
	}
	private void playRandom() {
		int currentIndex= new Random().nextInt(getStations().size()-1);
		currentStation = getStations().get(currentIndex);
		if(currentStation!=null) play(currentStation.url);
	}

	/**
	 * Init media session.
	 */
	public void initMediaSession() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			String TAG = "MyMediaSession";
			mediaSession = new MediaSession(getApplicationContext(), TAG);
			mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
			mediaSession.setActive(true);
			Intent intentMediaButton = new Intent(getApplicationContext(), ServiceRadio.class);
			intentMediaButton.setAction(Constants.INTENT_MEDIA_BUTTON);
			PendingIntent pendingIntent=PendingIntent.getService(getApplicationContext(), 0, intentMediaButton, 0);
			mediaSession.setMediaButtonReceiver(pendingIntent);
		}
	}

	/**
	 * Play.
	 *
	 * @param mRadioUrl the m radio url
	 */
	public void play(String mRadioUrl) {
		if(interruptHandler.requestFocus()) {
			if (mRadioUrl == null) mRadioUrl = currentStation.url;
			sendBroadcast(new Intent(Constants.ACTION_MEDIAPLAYER_STOP));
			notifier.notifyLoadingStarted(mRadioUrl);
			if (checkSuffix(mRadioUrl))
				decodeStremLink(mRadioUrl);
			else {
				currentStation.url = mRadioUrl;
				isSwitching = false;

				if (isPlaying()) {
					Log.d(TAG, "Switching Radio");
					isSwitching = true;
					stop();
				} else if (!mLock) {
					Log.d(TAG,"Play requested.");
					mLock = true;
					getPlayer().playAsync(mRadioUrl);
				}
			}
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		if (!mLock && mRadioState == State.PLAYING) {
			Log.d(TAG, "Stop requested.");
			mLock = true;
			mRadioState=State.IDLE;
			getPlayer().stop();
		}
	}

/*
    private void stop() {
        Log.d(TAG, "Stop requested.");
        if(isPlaying()) {
            getPlayer().stop();
        }
        if(isSwitching) isSwitching=false;
        if(!isClosedFromNotification()) {
            notifier.notifyPlaybackStopped(true);
        }
        mLock=false;
    }*/

	/**
	 * Is playing boolean.
	 *
	 * @return the boolean
	 */
	public boolean isPlaying(){
		if (State.PLAYING == mRadioState)
			return true;
		return false;
	}


	@Override
	public void onDestroy() {
		if(isPlaying()) stop();
		if(notifier!=null) notifier.unregisterAll();
		savePrefsToStorage();

		super.onDestroy();

	}


	/**
	 * Save prefs to storage.
	 */
	public void savePrefsToStorage(){
		Log.d(TAG, "savePrefsToStorage");

		Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				/*FileOutputStream fos = null;
				try {
					fos = openFileOutput(Constants.SHARED_PREFS_FILENAME, Context.MODE_PRIVATE);
					fos.flush();
					for(Station s: stations){
						if(s.fav) {
							String stringToWrite = s.url + "\n";
							Log.d(TAG, "write prefs: " + stringToWrite);
							fos.write(stringToWrite.getBytes());
						}
					}
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				//Set<String> statsStrings = settings.getStringSet("statsStrings", new HashSet<String>());


				SharedPreferences.Editor editor = getSharedPreferences(
						Constants.SHARED_PREFS_NAME, MODE_PRIVATE).edit();

				editor.putBoolean(Constants.SHARED_PREFS_IS_FAV, isFavOnly());

				Set<String> statsStrings =new HashSet<String>();
				for(Station s: stations){
					if(s.fav) {
						Log.d(TAG, "write prefs: " +  s.url);
						statsStrings.add(s.url);
					}
				}

				editor.putStringSet(Constants.SHARED_PREFS_FAV_STATIONS_SET, statsStrings);
				if(editor.commit()) subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.newThread())
				.observeOn(Schedulers.newThread())
				.subscribe();

	}

	/**
	 * Get prefs from storage boolean.
	 *
	 * @return the boolean
	 */
	public boolean getPrefsFromStorage(){
		boolean res=false;

		SharedPreferences prefs;
		if(getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE)!=null) {
			prefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
			setFavOnly(prefs.getBoolean(Constants.SHARED_PREFS_IS_FAV, false));
			Log.d(TAG, "Read prefs: favOnly="+isFavOnly());

			Set<String> statsStrings = prefs.getStringSet(Constants.SHARED_PREFS_FAV_STATIONS_SET, new HashSet<String>());
			stationsFav=new ArrayList<>();
			Log.d(TAG, "Read prefs: statsStrings.size="+statsStrings.size());
			Log.d(TAG, "stations.size()="+ stations.size());

			if(statsStrings.size()!=0){
				res=true;
				for(String s: statsStrings){
					Log.d(TAG, "fav station loaded "+ s);

					Station station= getStationByUrl(s);
					Log.d(TAG, "station == null -> "+ (station==null));

					stationsFav.add(station);
					stations.get(stations.indexOf(station)).fav=true;

				}
			}
		}/*
		FileInputStream fis = null;
		stationsFav=new ArrayList<>();
		try {
			if(new File(Constants.SHARED_PREFS_FILENAME).exists()){
				fis = openFileInput(Constants.SHARED_PREFS_FILENAME);

				StringBuilder fileContent = new StringBuilder("");
				byte[] buffer = new byte[1024];
				int n;
				while ((n = fis.read(buffer)) != -1)
					fileContent.append(new String(buffer, 0, n));
				String strResult = fileContent.toString();
				Log.d(TAG, "strResult: \n\t"+strResult);

				if(strResult.length()==0) {
					Log.d(TAG, "no saved favorites found");
					return false;
				}
					for (String s : strResult.split("\n")) {
						Log.d(TAG, "url: " + s);
						Station station=getStationByUrl(s);
						if(station!=null)station.fav=true;
						stationsFav.add(station);
						res=true;
					}
			} else {
				Log.d(TAG, "no saved favorites found");
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		getStationsListChangedNotification();
		return res;
	}

	public void getStationsListChangedNotification(){
		if(isFavOnly()) notifier.notifyListChanged(stationsFav);
		else notifier.notifyListChanged(stations);
	}

	/**
	 * Set closed from notification.
	 *
	 * @param isClosedFromNotification the is closed from notification
	 */
	public void setClosedFromNotification(boolean isClosedFromNotification){
		this.isClosedFromNotification=isClosedFromNotification;

	}

	/**
	 * Is closed from notification boolean.
	 *
	 * @return the boolean
	 */
	public boolean isClosedFromNotification(){
		return isClosedFromNotification;
	}


	/**
	 * Check suffix boolean.
	 *
	 * @param streamUrl the stream url
	 * @return the boolean
	 */
	public boolean checkSuffix(String streamUrl) {
		if (streamUrl.contains(SUFFIX_PLS) ||
				streamUrl.contains(SUFFIX_RAM) ||
				streamUrl.contains(SUFFIX_WAX))
			return true;
		else
			return false;
	}


	private void decodeStremLink(String streamLink) {
		new StreamLinkDecoder(streamLink) {
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
				mLock=false;
				play(s);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Gets stations.
	 *
	 * @return the stations
	 */
	public ArrayList<Station> getStations() {
		Log.d(TAG, "getStations. favOnly: "+ favOnly);
		if(favOnly) return stationsFav;
		else return stations;
	}

	/**
	 * Update stations.
	 *
	 * @param stationsUpdated the stations updated
	 */
	public void updateStations(ArrayList<Station> stationsUpdated) {
		stations=stationsUpdated;
	}

	/**
	 * Update station.
	 *
	 * @param s   the s
	 * @param fav the fav
	 */
	public void updateStation(Station s, boolean fav) {
		int i = stations.indexOf(s);
		stations.get(i).fav=fav;
	}

	/**
	 * Get station by url station.
	 *
	 * @param url the url
	 * @return the station
	 */
	public Station getStationByUrl(String url){
		for(Station s:stations){
			if (s.url.contains(url)){
				return s;
			}
		}
		return null;
	}


	/**
	 * Is fav only boolean.
	 *
	 * @return the boolean
	 */
	public boolean isFavOnly() {
		return favOnly;
	}

	/**
	 * Sets fav only.
	 *
	 * @param favOnly the fav only
	 */
	public void setFavOnly(boolean favOnly) {
		this.favOnly = favOnly;
	}

/*

    private void updateWidget()  {//TODO UPDATE WIDGET CODE
        Intent intent = new Intent(this, SimpleWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), SimpleWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        intent.putExtra(SimpleWidgetProvider.SELECTED_STATION_TITLE, radioManager.getCurrentStation().name);
        intent.putExtra(SimpleWidgetProvider.IS_PLAYING, radioManager.isPlaying());
        sendBroadcast(intent);
    }
    private void buildNotification() {

        Intent intentPlayPause = new Intent(NOTIFICATION_INTENT_PLAY_PAUSE);
        Intent intentPrev = new Intent(NOTIFICATION_INTENT_PREV);
        Intent intentNext = new Intent(NOTIFICATION_INTENT_NEXT);
        Intent intentOpenPlayer = new Intent(NOTIFICATION_INTENT_OPEN_PLAYER);
        intentOpenPlayer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent intentCancel = new Intent(NOTIFICATION_INTENT_CANCEL);

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
            artImage = BitmapFactory.decodeResource(getResources(), getArt(stationName, getApplicationContext()));
        mNotificationTemplate.setTextViewText(R.id.notification_station_name, stationName);
        mNotificationTemplate.setImageViewResource(R.id.notification_prev, R.drawable.ic_prev);
        mNotificationTemplate.setImageViewResource(R.id.notification_next, R.drawable.ic_next);
        mNotificationTemplate.setImageViewResource(R.id.notification_play,
                isPlaying() ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
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
    private void updateNotification(String stationName, String singerName, String songName, int smallImage, int artImage) {
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
    private void updateNotification(String stationName, String singerName, String songName, int smallImage, Bitmap artImage) {
        //TODO updateNotification
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




    private static int getArt(String fileName, Context c){
        int resID = c.getResources().getIdentifier(fileName, "drawable", c.getPackageName());
        if(resID!=0) return resID;
        else return R.drawable.default_art;
    }




    private void setSleepTimer(int seconds){
        if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
        sleepTimerTask=null;
        sleepTimerTask=new SleepTimerTask();
        sleepTimerTask.execute(seconds);
        notifySleepTimerStatusUpdate(SLEEP_TIMER_START,seconds);
        Log.d(TAG, "sleep timer created");
    }
    private void cancelSleepTimer(){
        if(sleepTimerTask!=null) sleepTimerTask.cancel(true);
        notifySleepTimerStatusUpdate(SLEEP_TIMER_CANCEL,-1);
        Log.d(TAG, "sleep timer cancelled");
    }
    private class SleepTimerTask extends AsyncTask<Integer, Integer, Void> {
        @Override
        protected void onProgressUpdate(Integer... values) {
            int secondsLeft = values[0];
            Log.d(TAG, "onProgressUpdate sleep timer left "+ secondsLeft +" seconds");
            notifySleepTimerStatusUpdate(SLEEP_TIMER_UPDATE, secondsLeft);
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

*/
}
