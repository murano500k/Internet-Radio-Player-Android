package com.stc.radio.player.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikepenz.materialize.MaterializeBuilder;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.BitmapManager;
import com.stc.radio.player.R;
import com.stc.radio.player.ServiceRadioRx;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.db.DbHelper.getPlaylistId;
import static com.stc.radio.player.ui.MainActivity.UI_STATE.IDLE;
import static com.stc.radio.player.ui.MainActivity.UI_STATE.LOADING;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity
		implements ListFragment.OnListFragmentInteractionListener
		, PlaybackControlsFragment.OnControlsFragmentInteractionListener
		, NavigationDrawerFragment.NavigationDrawerCallbacks
		, BitmapManager {
	public static final String TAG = "ActivityRadio";


	private static ServiceRadioRx serviceRadioRx;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private EventBus bus=EventBus.getDefault();
	public static final String INTENT_OPEN_APP = "com.stc.radio.player.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.stc.radio.player.INTENT_CLOSE_APP";

	private Subscription mSubscription, checkDbSubscription;
	private NavigationDrawerFragment fragmentDrawer;
	private PlaybackControlsFragment fragmentControls;
	private ListFragment fragmentList;
	private ProgressBar splashScreen;

	final public static class UI_STATE {
		public static final int LOADING= 0;
		public static final int PLAYING= 1;
		public static final int IDLE= -1;
	}
	public static final int INDEX_SHUFFLE=7;
	static final int INDEX_SLEEP=8;
	public static final int INDEX_RESET=9;
	public static final int INDEX_BUFFER=10;

	public DialogShower getDialogShower() {
		return dialogShower;
	}

	private Toast toast;
	Toolbar toolbar;
	private CharSequence mTitle;
	@Override

	public void onListFragmentInteraction(StationListItem item) {
		assertNotNull(item);
		Timber.d("StationListItem clicked %s", item.station.url);
		KeyboardUtil.hideKeyboard(getActivity());
		NowPlaying nowPlaying= DbHelper.getNowPlaying();
		nowPlaying.withUrl(item.station.url).withUiState(LOADING).withArtist("loading...").withSong(null).save();
		//if(fragmentControls!=null) fragmentControls=null;
		//initControlsFragment();
		progressBar.setIndeterminate(true);
		bus.post(item);
	}
	@Override
	public void onControlsFragmentInteraction(int value) {
		Timber.d("controls clicked %d", value);
		KeyboardUtil.hideKeyboard(getActivity());
		//assertNotNull(fragmentControls);
		//DbHelper.setPlayerState(UI_STATE.LOADING);
		//fragmentControls.onPlaybackStateChanged(UI_STATE.LOADING);
		if(value==0) {

			bus.post(0);
		}
		else if(value>0) {
			assertNotNull(fragmentList);
			fragmentList.selectNextItem();
		}
		else {
			assertNotNull(fragmentList);
			fragmentList.selectPrevItem();
		}
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		Timber.d("position= %d", position);
		loadingStarted();
		mSubscription = Observable.just(1)
				.doOnNext(i -> {
					loadingStarted();
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
										Timber.e(throwable, "Loading error");
										loadingFinished();
									}
								})
								.onErrorResumeNext(Observable.empty());//prevent observable from breaking
					}
				})
				.subscribe(new Action1<Integer>() {
					@Override
					public void call(Integer integer) {
						//drawer=initDrawer(savedInstanceState);
						long plsId= getPlaylistId(getString(R.string.title_section_di));
						if(position>0)  plsId=DbHelper.getPlaylistId(position);
						Timber.d("id= %d", plsId);
						assertTrue(plsId>0);
						DbHelper.setActivePlaylistId(plsId);
						initListFragment(plsId);
						if(!isServiceConnected()) connect();
						Timber.w("Loading success %d" , integer);
						if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
					}
				});
	}
	public void checkDbContent(){
		checkDbSubscription = Observable.just(1)
				.doOnNext(i -> {
					loadingStarted();
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
										loadingFinished();
									}
								})
								.onErrorResumeNext(Observable.empty());//prevent observable from breaking
					}
				})
				.subscribe(new Action1<Integer>() {
					@Override
					public void call(Integer integer) {
						initDrawerAndUI();
						if(!isServiceConnected()) connect();
						else updateUI();
						showToast("Loading success " + integer);
						Timber.w("Loading success %d" , integer);
						if(checkDbSubscription!=null && !checkDbSubscription.isUnsubscribed()) checkDbSubscription.unsubscribe();
					}
				});
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void downloadImages(FastItemAdapter fastAdapter){
		loadingStarted();
		List <Station>list=DbHelper.getPlaylistStations(DbHelper.getActivePlaylistId());
		assertNotNull(fastAdapter);
		assertTrue(list!=null && !list.isEmpty());
		for(Station s:list) {
			File file=getArtFile(s.artPath);
			if (file.exists()) {
				Bitmap bitmap=getArtBitmap(file);
				StationListItem stationListItem = new StationListItem().withStation(s).withIdentifier(s.getId()).withFavorite(s.favorite).withIcon(bitmap);
				fastAdapter.add(stationListItem);
				stationListItem.station.active = true;
				stationListItem.station.position = fastAdapter.getGlobalPosition(fastAdapter.getAdapterPosition(stationListItem));
				fastAdapter.notifyAdapterItemInserted(stationListItem.station.position);
				//if(fragmentList!=null) fragmentList.onImageLoaded(s, bitmap);
			}else {
				Target target = new Target() {
					@Override
					public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
						Timber.d("From %s", from.toString());
						insertListItemWithBitmap(s,bitmap,fastAdapter);
					}

					@Override
					public void onBitmapFailed(Drawable errorDrawable) {
						Timber.e("From %s", errorDrawable.toString());
						insertListItemWithBitmap(s,drawableToBitmap(errorDrawable),fastAdapter);
					}

					@Override
					public void onPrepareLoad(Drawable placeHolderDrawable) {
					}
				};
				Picasso.with(this).load(DbHelper.getArtUrl(s.url)).error(getDrawable(R.drawable.default_art)).into(target);
			}
		}

		updateUI();

	}
	public void insertListItemWithBitmap (Station s, Bitmap bitmap,  FastItemAdapter fastAdapter){
		StationListItem stationListItem = new StationListItem().withStation(s).withIdentifier(s.getId()).withFavorite(s.favorite).withIcon(bitmap);
		fastAdapter.add(stationListItem);
		stationListItem.station.active = true;
		stationListItem.station.position = fastAdapter.getGlobalPosition(fastAdapter.getAdapterPosition(stationListItem));
		stationListItem.station.save();
		fastAdapter.notifyAdapterItemInserted(stationListItem.station.position);
		Observable.just(bitmap).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(bitmap1 -> {
			try {
				File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()
						+ "/" + s.artPath);
				if (!file.exists()) {
					assertNotNull(file.createNewFile());
				}
				FileOutputStream ostream = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
				ostream.close();
				Timber.d("artPath for %s =  %s", s.name, s.artPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiStateChanged(UiEvent event) {
		Timber.d("onUiStateChanged %d %s %s", DbHelper.getNowPlaying().getUiState(), DbHelper.getNowPlaying().getUrl(), DbHelper.getNowPlaying().getArtist());

		updateUI();
		/*assertNotNull(event);
		UiEvent.UI_ACTION action =event.getUiAction();
		Timber.d("onUiStateChanged %s", action);
		assertNotNull(fragmentList);
		int uiState = UI_STATE.IDLE;
		//String artist = "loading...";
		//assertEquals(fragmentList.getSelectedItem().getStation().url, event.getExtras().url);
		switch(DbHelper.getNowPlaying().getUiState()){
			case MainActivity.UI_STATE.PLAYING:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_pause_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case MainActivity.UI_STATE.IDLE:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_notif_play_new);
				notification.flags = 0;
				notification.ledOnMS = 0;
				break;
			case  MainActivity.UI_STATE.LOADING:
				notification.contentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.bigContentView.setImageViewResource(R.id.notification_play, R.drawable.ic_loading);
				notification.ledARGB = 0xFF00FF7F;
				notification.flags = Notification.FLAG_SHOW_LIGHTS*//* | Notification.FLAG_FOREGROUND_SERVICE*//*;
				notification.ledOnMS = 100;
				notification.ledOffMS = 100;
				break;
		}
		*//*switch(action){
			case PLAYBACK_STARTED:
				progressBar.setIndeterminate(false);
				uiState=UI_STATE.PLAYING;
				break;
			case PLAYBACK_STOPPED:
				progressBar.setIndeterminate(false);
				uiState=UI_STATE.IDLE;
				break;
			case LOADING_STARTED:
				progressBar.setIndeterminate(true);
				uiState=UI_STATE.LOADING;

				break;*//*

		Metadata metadata=new Metadata(null,null,event.getExtras().url);
		DbHelper.setMetadata(metadata);
		DbHelper.setUrl(event.getExtras().url);
		DbHelper.setPlayerState(uiState);

		if(fragmentList!=null && fragmentList.getSelectedItem()==null || !fragmentList.getSelectedItem().station.url.contains(event.getExtras().url)) {
			fragmentList.restoreState(DbHelper.getActivePosition());
		}
		if(fragmentControls==null || fragmentControls.getUrl()==null || !fragmentControls.getUrl().contains(event.getExtras().url))
			initControlsFragment();
		else {
			fragmentControls.updateButtons(uiState);
			fragmentControls.updateMetadata(metadata);
		}*/
	}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMetadataChanged(Metadata metadata) {
		assertNotNull(metadata);
		Timber.d("onMetadataChanged %s, %s", metadata.getArtist(), metadata.getSong());
		//DbHelper.setMetadata(metadata);
		initControlsFragment();
		//else fragmentControls.updateMetadata(metadata);
	}



	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBufferUpdate(BufferUpdate bufferUpdate) {
		assertNotNull(bufferUpdate);
		progressBar.setIndeterminate(false);
		progressBar.setProgress(bufferUpdate.getAudioBufferSizeMs() * progressBar.getMax() /
				bufferUpdate.getAudioBufferCapacityMs());
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Timber.i("check");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		splashScreen = (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.GONE);
		connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		bus.register(this);
		ButterKnife.bind(this);
		new MaterializeBuilder().withActivity(this).build();
		checkDbContent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().contains(INTENT_OPEN_APP))
			Timber.v("onNewIntent");
		else if (intent.getAction().contains(INTENT_CLOSE_APP)) {
			if(isServiceConnected()) this.unbindService(mServiceConnection);
			finish();
		} else if (intent.getAction().contains(ServiceRadioRx.INTENT_USER_ACTION)) {
			Timber.w("INTENT_USER_ACTION %s", intent);
			if(intent.getExtras().containsKey(ServiceRadioRx.EXTRA_PREV_PRESSED)) fragmentList.selectPrevItem();
			else if(intent.getExtras().containsKey(ServiceRadioRx.EXTRA_NEXT_PRESSED)) fragmentList.selectNextItem();
		}
		super.onNewIntent(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);
		menu.findItem(R.id.search).setIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_search).color(Color.LTGRAY).actionBar());

		final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String s) {
				if(fragmentList!=null){
					fragmentList.filter(s);
				}
				KeyboardUtil.hideKeyboard(getActivity());
				return true;
			}
			@Override
			public boolean onQueryTextChange(String s) {
				if(fragmentList!=null){
					fragmentList.filter(s);
				}
				//touchCallback.setIsDragEnabled(TextUtils.isEmpty(s));
				return true;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		KeyboardUtil.hideKeyboard(getActivity());
		if (fragmentDrawer != null && fragmentDrawer.isDrawerOpen()) {
			fragmentDrawer.updateDrawerState(false);
		} else{
			super.onBackPressed();
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//Timber.i("check");
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		//Timber.i("check");
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onPause(){
		//Timber.i("check");
		/*if(fragmentControls!=null) {
			NowPlaying nowPlaying= DbHelper.getNowPlaying();
			nowPlaying.withArtist(fragmentControls.getArtist())
					.withSong(fragmentControls.getSong())
					.save();
		}*/

		super.onPause();
	}

	@Override
	protected void onResume(){
		super.onResume();
		if(!bus.isRegistered(this))bus.register(this);
		//Timber.i("check");
		Timber.i("onResume, pls %b, stations %b", DbHelper.checkIfPlaylistsExist() , DbHelper.checkIfStationsExist());
		if(DbHelper.checkIfPlaylistsExist() && DbHelper.checkIfStationsExist()) updateUI();
	}
	@Override
	protected void onStop() {
		//Timber.i("check");
		if(bus.isRegistered(this))bus.unregister(this);
		if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
		super.onStop();
	}
	@Override
	protected void onStart() {
		//Timber.i("check");
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		//Timber.i("check");
		super.onDestroy();
	}

	private void initListFragment(long plsId){
		DbHelper.setActivePlaylistId(plsId);
		Timber.d("plsId=%d", plsId);
		fragmentList= ListFragment.newInstance(plsId);
		assertNotNull(fragmentList);
		FragmentManager fragmentManager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.container_list, fragmentList);
		ft.commit();
	}

	private void initControlsFragment(){
		Timber.d("initControlsFragment");
		Station station = DbHelper.getActiveStation();
		if(station==null){
			Timber.e("initControlsFragment no station selected");
			fragmentControls=null;
			return;
		}else {
			Timber.w("new art %s %s", station.name, station.artPath);
			File file = getArtFile(station.artPath);
			if (file.exists()) {
				//bus.post(getArtBitmap(file));
				//if(fragmentControls!=null && bus.isRegistered(fragmentControls)) bus.unregister(fragmentControls);
				/*setPlayerState(state);
				setUrl(url);
				setMetadata(new Metadata(artist, song, url));*/
				fragmentControls = PlaybackControlsFragment.newInstance();
				assertNotNull(fragmentControls);
				FragmentManager fragmentManager = getSupportFragmentManager();
				android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
				ft.replace(R.id.container_controls, fragmentControls);
				ft.commit();
			} else {
				fragmentControls=null;
				Timber.e("NO IMAGE FOR CONTROLS FRAGM");
			}
		}
	}
	public void initDrawerAndUI() {
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		dialogShower = new DialogShower();
		fragmentDrawer = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		if (fragmentDrawer == null) {
			throw new IllegalStateException("Mising fragment with id 'fragment_naivgation_drawer'. Cannot continue.");
		}
		fragmentDrawer.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout), (int) DbHelper.getActivePlaylistId());
		if(DbHelper.getActivePlaylist()!=null) {
			mTitle = DbHelper.getActivePlaylist().name;
			//if(fragmentDrawer.isDrawerOpen()) fragmentDrawer.onDr
		}
		else mTitle="OnlineRadio";
		restoreActionBar();
	}
	private void loadingStarted(){
		showToast("Loading started");
		//Timber.w("Loading started");
		if(splashScreen==null) splashScreen= (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.VISIBLE);
		setTitle("LOADING...");
	}
	public void loadingFinished(){
		hideToast();
		//Timber.w("loadingFinished");
		if(!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
		}
		if(DbHelper.getActivePlaylist()!=null)
			mTitle=DbHelper.getActivePlaylist().name;
		else mTitle="OnlineRadio";
		restoreActionBar();
		if(splashScreen==null) splashScreen= (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.GONE);
	}

	public void updateUI(){
		Timber.w("updateUI %d %s %s", DbHelper.getNowPlaying().getUiState(), DbHelper.getNowPlaying().getUrl(), DbHelper.getNowPlaying().getArtist());
		initControlsFragment();
		NowPlaying nowPlaying =DbHelper.getNowPlaying();
		if(progressBar == null) 		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		if(nowPlaying.getUiState()==LOADING){
			progressBar.setIndeterminate(true);
		}else if(nowPlaying.getUiState()==IDLE){
			progressBar.setProgress(0);
			progressBar.setIndeterminate(false);
		}else progressBar.setIndeterminate(false);
		/*if(nowPlaying.url!=null && DbHelper.getActiveStation()!=null) {
			if (fragmentControls == null || fragmentControls.getUrl() == null || !nowPlaying.url.contains(fragmentControls.getUrl())) {
				if (getArtFile(DbHelper.getActiveStation().artPath).exists())

				else Timber.e("ERROR art not found");
			} else if(fragmentControls!=null){

				fragmentControls.updateMetadata(nowPlaying.artist, nowPlaying.song);
				fragmentControls.updateButtons(nowPlaying.getUiState());
			}
		}*/
		if (fragmentList == null) Timber.e("ERROR listfrag is null");
		else {
			//Timber.w("activePos = %s", DbHelper.getActivePosition());
			if (DbHelper.getActivePosition() >= 0) {
				fragmentList.restoreState(DbHelper.getActivePosition());
			}else Timber.e("getActivePosition %d", DbHelper.getActivePosition());
		}
		//restoreActionBar();
		loadingFinished();
	}
	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if(actionBar!=null){
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(mTitle);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
	}

		/*
		int selectedPlsPos=;
		if(savedInstanceState!=null) selectedPlsPos=savedInstanceState.getInt(SELECTED_PLAYLIST_POSITION, 0);
		else selectedPlsPos=getPrefs().getInt(SELECTED_PLAYLIST_POSITION,0);
		if(selectedPlsPos!=fragmentDrawer.getSelectedPosition()) */
		/*btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrev = (ImageButton) findViewById(R.id.btnPrev);
		spinner = (AVLoadingIndicatorView) findViewById(R.id.spinner);
		btnPlay = (ImageButton) findViewById(R.id.buttonControlStart);
		btnPlay.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED));
		btnPrev.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_PREV_PRESSED));
		btnNext.setOnClickListener(v -> btnControlClick(ServiceRadioRx.EXTRA_NEXT_PRESSED));
		btnPlay.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		btnPrev.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		btnNext.setOnTouchListener((view, motionEvent) -> {
			if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_PRESS) view.setPressed(true);
			else if(motionEvent.getAction()==MotionEvent.ACTION_BUTTON_RELEASE) view.setPressed(false);
			return false;
		});
		infoView=findViewById(R.id.info_layout);
		infoStation=(TextView) infoView.findViewById(R.id.textViewStation);
		infoArtist=(TextView) infoView.findViewById(R.id.textViewArtist);
		infoSong=(TextView) infoView.findViewById(R.id.textViewSong);
		infoArt=(ImageView) infoView.findViewById(R.id.imageViewArt);
		infoProgress = (TextView) infoView.findViewById(R.id.textViewProgress);*/

		/*fragmentDrawer = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		if (fragmentDrawer == null) {
			throw new IllegalStateException("Mising fragment with id 'navigation_drawer'. Cannot continue.");
		}*//*
		mTitle = getTitle();
		fragmentDrawer.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));*/
		//showPlaybackControls();











	private boolean isPlaying() {
		return serviceRadioRx != null && serviceRadioRx.isPlaying();
	}

	public ServiceRadioRx getService() {
		return serviceRadioRx;
	}

	public boolean isServiceConnected() {
		return (serviceRadioRx!=null && serviceRadioRx.isServiceConnected);
	}

	public ConnectivityManager connectivityManager;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.i(TAG, "Service Connected.");
			serviceRadioRx = ((ServiceRadioRx.LocalBinder) binder).getService();
			serviceRadioRx.isServiceConnected=true;
			Timber.v("connected %b", isServiceConnected());
			if(!serviceRadioRx.isPlaying()){
				DbHelper.getNowPlaying().withUiState(IDLE).withSong(null).withArtist(null).save();
			}
			updateUI();
		}
		//exp_internal -f ~/exp.log
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceRadioRx.isServiceConnected=false;
			DbHelper.getNowPlaying().withUiState(IDLE).withSong(null).withArtist(null).save();
			updateUI();
		}
	};
	public void connect() {
		Intent intent = new Intent(this, ServiceRadioRx.class);
		this.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	public boolean isNetworkConnected(){
		if(connectivityManager==null) connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (connectivityManager.getActiveNetworkInfo()==null) return false;
		else return (connectivityManager.getActiveNetworkInfo().isConnected());
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
	public void hideToast(){
		Observable.just(1)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(integer -> {
					if(toast!=null){
						toast.cancel();
						toast=null;
					}
				});
	}
	public MainActivity getActivity(){
		return this;
	}


	public File getArtFile(String name){
		return new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()
				+ "/"+name);
	}
	public Bitmap getArtBitmap(File file){
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
		return bitmap;
	}
	public Bitmap drawableToBitmap (Drawable drawable) {
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


