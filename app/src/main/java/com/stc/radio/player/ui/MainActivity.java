package com.stc.radio.player.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
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

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikepenz.materialize.MaterializeBuilder;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.R;
import com.stc.radio.player.ServiceRadioRx;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.db.DbHelper.getPlaylistId;
import static com.stc.radio.player.db.DbHelper.setMetadata;
import static com.stc.radio.player.db.DbHelper.setPlayerState;
import static com.stc.radio.player.db.DbHelper.setUrl;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity
		implements ListFragment.OnListFragmentInteractionListener
		, PlaybackControlsFragment.OnControlsFragmentInteractionListener
		, NavigationDrawerFragment.NavigationDrawerCallbacks {
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
		Timber.d("StationListItem clicked %s", item.station.name);
		KeyboardUtil.hideKeyboard(getActivity());
		NowPlaying nowPlaying= DbHelper.getNowPlaying();
		nowPlaying.withUrl(item.station.url).save();
		initControlsFragment(item.station.url, null, null, UI_STATE.LOADING);
		progressBar.setIndeterminate(true);
		bus.post(item);
	}
	@Override
	public void onControlsFragmentInteraction(int value) {
		Timber.d("controls clicked %d", value);
		KeyboardUtil.hideKeyboard(getActivity());

		assertNotNull(fragmentControls);
		fragmentControls.onPlaybackStateChanged(UI_STATE.LOADING);
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
						initListFragment(plsId);
						if(!isServiceConnected()) connect();
						loadingFinished();
						Timber.w("Loading success %d" , integer);
						if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
					}
				});
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiStateChanged(UiEvent event) {
		assertNotNull(event);
		UiEvent.UI_ACTION action =event.getUiAction();
		Timber.d("onUiStateChanged %s", action);
		assertNotNull(fragmentList);
		int uiState = UI_STATE.IDLE;
		//assertEquals(fragmentList.getSelectedItem().getStation().url, event.getExtras().url);
		switch(action){
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
				break;
		}
		if(fragmentControls==null || !fragmentControls.getUrl().contains(event.getExtras().url))
			initControlsFragment(
					event.getExtras().url,
					event.getExtras().artist,
					event.getExtras().song,
					uiState);
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
		splashScreen.setVisibility(View.GONE);
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
		/*outState.putInt(SELECTED_PLAYLIST_POSITION, fragmentDrawer.getSelectedPosition());
		getPrefs().edit().putInt(SELECTED_PLAYLIST_POSITION, fragmentDrawer.getSelectedPosition()).apply();
		*/Timber.i("check");
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Timber.i("check");
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onPause(){
		Timber.i("check");
		super.onPause();
	}
	@Override
	protected void onResume(){
		Timber.i("check");

		super.onResume();
		updateUI();
	}
	@Override
	protected void onStop() {
		Timber.i("check");
		if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
		super.onStop();
	}
	@Override
	protected void onStart() {
		Timber.i("check");
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		Timber.i("check");
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

	private void initControlsFragment(String url, String artist, String song, int state){
		Timber.d("initControlsFragment");
		//if(fragmentControls!=null && bus.isRegistered(fragmentControls)) bus.unregister(fragmentControls);
		setPlayerState(state);
		setUrl(url);
		setMetadata(new Metadata(artist,song, url));
		fragmentControls= PlaybackControlsFragment.newInstance(fragmentControls, url);
		assertNotNull(fragmentControls);
		FragmentManager fragmentManager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.container_controls, fragmentControls);
		ft.commit();
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
				(DrawerLayout) findViewById(R.id.drawer_layout), (int) DbHelper.getCurrentPlaylistId());

		restoreActionBar();
	}
	private void loadingStarted(){
		if(EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().unregister(this);
		}
		showToast("Loading started");
		Timber.w("Loading started");
		splashScreen.setVisibility(View.VISIBLE);
		mTitle=getTitle();
		setTitle("LOADING...");
	}
	public void loadingFinished(){
		hideToast();
		Timber.w("loadingFinished");
		if(!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
		}
		mTitle=DbHelper.getPlaylistName(DbHelper.getCurrentPlaylistId());
		restoreActionBar();
		updateUI();
		splashScreen.setVisibility(View.GONE);
	}

	public void updateUI(){
		NowPlaying nowPlaying =DbHelper.getNowPlaying();
		if(fragmentControls==null) initControlsFragment(nowPlaying.getUrl(), nowPlaying.getArtist(), nowPlaying.getSong(), UI_STATE.LOADING);
		else fragmentControls.onPlaybackStateChanged(nowPlaying.getUiState());
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
						loadingFinished();
						showToast("Loading success " + integer);
						Timber.w("Loading success %d" , integer);
						if(checkDbSubscription!=null && !checkDbSubscription.isUnsubscribed()) checkDbSubscription.unsubscribe();
					}
				});
	}

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
			loadingFinished();
			updateUI();
			Timber.v("connected %b", isServiceConnected());
		}
		//exp_internal -f ~/exp.log
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceRadioRx.isServiceConnected=false;
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


}


