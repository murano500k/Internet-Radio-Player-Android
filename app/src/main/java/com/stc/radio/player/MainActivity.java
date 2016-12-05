package com.stc.radio.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.materialize.MaterializeBuilder;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.contentmodel.StationsManager;
import com.stc.radio.player.db.Metadata;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.DialogShower;
import com.stc.radio.player.utils.PabloPicasso;
import com.stc.radio.player.utils.SettingsProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.stc.radio.player.contentmodel.StationsManager.PLAYLISTS.FAV;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity
		implements ListFragment.OnListFragmentInteractionListener
		, PlaybackControlsFragment.OnControlsFragmentInteractionListener
		, NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = "ActivityRadio";
	private static final String INTERRUPTED_LOADING_PLAYLIST = "com.stc.radio.player.INTERRUPTED_LOADING_PLAYLIST";
	public static final String EXTRA_START_FULLSCREEN = "com.stc.radio.player.EXTRA_START_FULLSCREEN";


	private static ServiceRadioRx serviceRadioRx;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private EventBus bus=EventBus.getDefault();
	public static final String INTENT_OPEN_APP = "com.stc.radio.player.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.stc.radio.player.INTENT_CLOSE_APP";
	public static final String INTENT_SERVICE_READY = "com.stc.radio.player.INTENT_SERVICE_READY";
	public static final String INTENT_DB_READY = "com.stc.radio.player.INTENT_DB_READY";

	private Subscription mSubscription, checkDbSubscription;
	private NavigationDrawerFragment fragmentDrawer;
	private PlaybackControlsFragment fragmentControls;
	private ListFragment fragmentList;
	private ProgressBar splashScreen;
	private AlertDialog loadingDialog;
	private boolean shouldCreateFragments;
	private NowPlaying nowPlaying;
	boolean loadingState =false;

	public static final int INDEX_SHUFFLE=7;
	static final int INDEX_SLEEP=8;
	public static final int INDEX_RESET=9;
	public static final int INDEX_BUFFER=10;
	private boolean isLoading;

	public DialogShower getDialogShower() {
		return dialogShower;
	}

	private Toast toast;
	Toolbar toolbar;
	private CharSequence mTitle;

	rx.Observer<StationListItem> listUpdateObserver;
	rx.Subscription listUpdateSubscription;

	@Override
	public void onListFragmentInteraction(StationListItem item) {
		assertNotNull(item);
		//if(toolbar!=null && item.getStation()!=null ) toolbar.setTitle(item.getStation().getPlaylist());
		KeyboardUtil.hideKeyboard(getActivity());

		nowPlaying.setStation(item.getStation());
		bus.post(nowPlaying.getStation());
	}

	/*Metadata string: StreamTitle='Pete Seeger - I Celebrate Life (Spoken Word)';StreamUrl='';*/
	/* Metadata string: StreamTitle='Rhian Sheehan - Boundaries (Module 'Evil Eno' Remix)';StreamUrl='';*/
	@Override
	public void onControlsFragmentInteraction(int value) {
		Timber.d("controls clicked %d", value);
		KeyboardUtil.hideKeyboard(getActivity());
		Intent intent = new Intent(Context.NOTIFICATION_SERVICE);
		intent.setClass(serviceRadioRx.getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(ServiceRadioRx.EXTRA_WHICH, value);
		startService(intent );
	}
	public rx.Observer<StationListItem> getListObserver(){
		return new rx.Observer<StationListItem>() {

			@Override
			public void onCompleted() {
				runOnUiThread(() -> {
					nowPlaying=NowPlaying.getInstance();
					if(fragmentControls==null)initControlsFragment();
					updateLoadingState(false);
					FastItemAdapter<StationListItem> adapter = fragmentList.getAdapter();
					adapter.notifyAdapterDataSetChanged();
					checkService();
				});
				Timber.w("SUCCESS");
			}

			@Override
			public void onError(Throwable e) {
				Timber.e(e,"FAIL");
			}

			@Override
			public void onNext(StationListItem stationListItem) {
				if(stationListItem.getStation()==null) {
					Timber.e("NULL STATION");
					throw new RuntimeException("NULL STATION");
				}
				Timber.w("onAdapterAddItem %s",stationListItem.getStation().getName());
				runOnUiThread(() -> {
					FastItemAdapter<StationListItem> adapter = fragmentList.getAdapter();
					adapter.add(stationListItem);
					int pos = adapter.getAdapterPosition(stationListItem);
					adapter.notifyAdapterItemInserted(pos);
				});
			}
		};
	}
	public Subscription observePlsUpdate(String pls){
		if(!SettingsProvider.getPlaylist().equals(pls)) SettingsProvider.setPlaylist(pls);
		if (listUpdateObserver == null) {
			listUpdateObserver = getListObserver();
		}
		loadingStarted();
		Timber.w("name %s", pls);
		return StationsManager.getPlsUpdateObservable(pls).doOnSubscribe(new Action0() {
			@Override
			public void call() {
				runOnUiThread(() -> {
					if (fragmentList == null) initListFragment();
					FastItemAdapter<StationListItem> adapter = fragmentList.getAdapter();
					if(adapter.getAdapterItems()!=null) for(StationListItem item: adapter.getAdapterItems())
						PabloPicasso.with(getApplicationContext()).cancelTag(item.getStation().getKey());
					adapter.clear();
					adapter.notifyAdapterDataSetChanged();
				});
			}
		}).subscribe(listUpdateObserver);
	}

	@Override
	public void onNavigationDrawerItemSelected(String pls) {
		Timber.d("onNavigationDrawerItemSelected name= %s", pls);
		if(listUpdateSubscription!=null && !listUpdateSubscription.isUnsubscribed()) {
			showToast("Please wait");
		}else if(pls==null) {
			showToast("Please wait");
			Timber.e(pls);
		}else {

			if(fragmentDrawer!=null )fragmentDrawer.updateDrawerState(false);
			mTitle=pls;
			updateLoadingState(true);
			listUpdateSubscription=observePlsUpdate(pls);
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBufferUpdate(BufferUpdate bufferUpdate) {
		assertNotNull(bufferUpdate);
		progressBar.setProgress(bufferUpdate.getAudioBufferSizeMs() * progressBar.getMax() /
				bufferUpdate.getAudioBufferCapacityMs());
		}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNowPlayingUpdate(NowPlaying nowPlaying) {
		this.nowPlaying = nowPlaying;

		Timber.w("onNowPlayingUpdate: %d %s", nowPlaying.getStatus(), nowPlaying.getStation());
		if (nowPlaying.getStation() == null) hidePlaybackControls();
		else if(fragmentControls==null){
			initControlsFragment();
		}
		else if (!nowPlaying.getStation().equals(fragmentControls.getStation())
				|| nowPlaying.getStatus() != fragmentControls.getStatus()) {
			showPlaybackControls();
			this.nowPlaying = nowPlaying;
		}
	}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMetadataUpdate(Metadata metadata) {
		this.nowPlaying.setMetadata(metadata, false);
		if(fragmentControls==null){
			initControlsFragment();
		}
		else if(metadata==null || !metadata.equals(fragmentControls.getMetadata())) {
			showPlaybackControls();
		}
	}
	public void hidePlaybackControls(){
		Timber.w("check");

		fragmentList = ListFragment.newInstance();
		assertNotNull(fragmentList);
		FragmentManager fragmentManager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.hide(fragmentControls);
		ft.commit();
	}
	public void showPlaybackControls(){
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentControls==null) initControlsFragment();
		else {
			fragmentControls.setMetadata(nowPlaying.getMetadata());
			fragmentControls.setStation(nowPlaying.getStation());
			fragmentControls.setStatus(nowPlaying.getStatus());
		}
		Timber.w("check isHidden: %b", fragmentControls.isHidden());

		if(fragmentControls.isHidden()) assertTrue(fragmentManager.beginTransaction().show(fragmentControls).commit()>0);
		else fragmentControls.onContentUpdate();
	}


	private void loadingStarted(){
		isLoading=true;
		updateLoadingState(true);
	}
	public void loadingFinished(){
		isLoading=false;
		updateLoadingState(false);
	}


	private void updateLoadingState(boolean isLoading) {
		if (splashScreen == null)
			splashScreen = (ProgressBar) findViewById(R.id.progress_splash);
		if (progressBar == null)
			progressBar = (ProgressBar) findViewById(R.id.progressBar);


		this.isLoading=isLoading;

		progressBar.setIndeterminate(isLoading);
		if(isLoading) {
			Timber.w("Loading started");
			if(bus.isRegistered(this)) bus.unregister(this);
			if (fragmentControls!= null) fragmentControls.hide();
		}else {
			if(mTitle!=null)toolbar.setTitle(mTitle);
			Timber.w("Loading finished");
			hideToast();
			if(!bus.isRegistered(this)) bus.register(this);

			restoreActionBar();

		}

	}


	private void initUI() {
		if(nowPlaying==null) nowPlaying=NowPlaying.getInstance();
		assertNotNull(nowPlaying);
		if(!shouldCreateFragments)return;
		shouldCreateFragments=false;
		if(loadingDialog==null){
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setCancelable(false).setTitle("Loading stations");
			loadingDialog=b.create();
			loadingDialog.show();
		}
		Timber.w("active name %s", SettingsProvider.getPlaylist());
		initDrawer(SettingsProvider.getPlaylist());
		initListFragment();
		initControlsFragment();
		SettingsProvider.setDbExistsTrue();
		if(!bus.isRegistered(this))bus.register(this);
		if(loadingDialog!=null && loadingDialog.isShowing()) loadingDialog.cancel();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Timber.i("check");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		shouldCreateFragments=true;
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		splashScreen = (ProgressBar) findViewById(R.id.progress_splash);
		splashScreen.setVisibility(View.GONE);
		connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		new MaterializeBuilder().withActivity(this).build();
		initListFragment();

	}
	private String initNowPlaying(){
		String pls = SettingsProvider.getPlaylist();
		nowPlaying=NowPlaying.getInstance();
		if (nowPlaying==null || nowPlaying.getStation()==null){
			From from;
			Timber.w("DB empty. Now will download");
			if(pls==FAV) from = new Select().from(Station.class).where("Favorite = ?", true);
			else from = new Select().from(Station.class).where("Playlist = ?", pls);
			if(from.exists()) nowPlaying.setStation(from.executeSingle(),true);
		}

		if(fragmentDrawer!=null) fragmentDrawer.selectItem(pls);
		else initDrawer(pls);
		return pls;
	}

	private void checkService() {
		loadingStarted();
		if(serviceRadioRx==null || !serviceRadioRx.isServiceConnected) {
			connect();
		}else loadingFinished();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().contains(INTENT_OPEN_APP))
			Timber.v("onNewIntent");
		else if (intent.getAction().contains(INTENT_CLOSE_APP)) {
			if(isServiceConnected()) this.unbindService(mServiceConnection);
			nowPlaying.withMetadata(null).setStatus(NowPlaying.STATUS_IDLE,false);
			onNowPlayingUpdate(nowPlaying);

			finish();
		}else if (intent.getAction().contains(INTENT_SERVICE_READY)) {
			Timber.v("INTENT_SERVICE_READY");
			initUI();
		}
		else if (intent.getAction().contains(INTENT_DB_READY)) {
			Timber.v("INTENT_DB_READY");
			checkService();
		}
		super.onNewIntent(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);
		menu.findItem(R.id.search).setIcon(getDrawable(android.R.drawable.ic_menu_search));

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
		Timber.i("check");

		if(listUpdateSubscription!=null && !listUpdateSubscription.isUnsubscribed()) {
			listUpdateSubscription.unsubscribe();
			outState.putBoolean(INTERRUPTED_LOADING_PLAYLIST, true);
		}else 			outState.putBoolean(INTERRUPTED_LOADING_PLAYLIST, false);
		//Timber.i("check");
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Timber.i("check");


			/*if(fragmentList==null || fragmentList.getAdapter()==null || fragmentList.getAdapter().getAdapterItems()==null
					|| fragmentList.getAdapter().getAdapterItems().size()==0) {

				listUpdateSubscription.unsubscribe();
				listUpdateSubscription = observePlsUpdate(SettingsProvider.getPlaylist());
				savedInstanceState.putBoolean(INTERRUPTED_LOADING_PLAYLIST, false);
			}
			if(listUpdateSubscription!=null && !listUpdateSubscription.isUnsubscribed()) {
			}*/

		//Timber.i("check");
	}
	@Override
	protected void onPause(){
		Timber.i("check");

		//Timber.i("check");
		/*if(fragmentControls!=null) {
			NowPlaying nowPlaying= DbHelper.getNowPlaying();
			nowPlaying.withArtist(fragmentControls.getArtist())
					.withSong(fragmentControls.getSong())
					.save();
		}*/
		if(listUpdateSubscription!=null && !listUpdateSubscription.isUnsubscribed()) {
			listUpdateSubscription.unsubscribe();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Timber.i("check");

		if((fragmentList.getAdapter().getAdapterItems()==null || fragmentList.getAdapter().getAdapterItems().isEmpty()) && isServiceConnected() && (listUpdateSubscription==null || listUpdateSubscription.isUnsubscribed())) {
			listUpdateSubscription=observePlsUpdate(SettingsProvider.getPlaylist());
		}else if(nowPlaying==null){
			String pls = initNowPlaying();
			mTitle=pls;
		}else if(getService().getNowPlaying()==null || getService().getNowPlaying().getStation()==null){
			Timber.e("NOWPLAYING NULL IN SERVICE");
		}else nowPlaying=getService().getNowPlaying();
		/*if(isServiceConnected() && NowPlaying.getInstance()!=null ){
		nowPlaying=NowPlaying.getInstance();
		onNowPlayingUpdate(nowPlaying);
		}*/
	}
		@Override
	protected void onStop() {
		//Timber.i("check");
		//if(bus.isRegistered(this))bus.unregister(this);
		//if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
		super.onStop();
	}
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onDestroy() {
		if(isServiceConnected()) disconnect();
		super.onDestroy();
	}

	private void initListFragment(){
		//rx.Observable.just()
		Timber.d("initListFragment");
		//
		//if(fragmentList==null) {
			/*List<Station> list = nowPlaying.getActiveList();
			DbHelper.trannsformToStations();
		assertNotNull(list);*/
			fragmentList = ListFragment.newInstance();
			assertNotNull(fragmentList);
			FragmentManager fragmentManager = getSupportFragmentManager();
			android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
			ft.replace(R.id.container_list, fragmentList);
			ft.commit();
		//}
	}

	private void initControlsFragment() {
		Timber.d("initControlsFragment");
		if(fragmentControls==null) {
			fragmentControls = PlaybackControlsFragment.newInstance();
			assertNotNull(fragmentControls);
			FragmentManager fragmentManager = getSupportFragmentManager();
			android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
			ft.replace(R.id.container_controls, fragmentControls);
			ft.commit();
			if(fragmentControls!=null && nowPlaying!=null && nowPlaying.getStation()!=null  && nowPlaying.getStation().getName()!=null) {
				Timber.w("nowPlaying %s", nowPlaying);
				Timber.w("nowPlaying station %s", nowPlaying.getStation().toString());

/*				fragmentControls.updateStation(nowPlaying.getStation());
				fragmentControls.updateButtons(nowPlaying.getStatus());
				fragmentControls.updateMetadata(nowPlaying.getMetadata());*/
			}
		}
	}


	public void initDrawer(String pls) {
		Timber.w("init drawer name %s", pls);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		dialogShower = new DialogShower();
		if(fragmentDrawer!=null) return;
		fragmentDrawer = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		if (fragmentDrawer == null) {
			throw new IllegalStateException("Mising fragment with id 'fragment_naivgation_drawer'. Cannot continue.");
		}
		fragmentDrawer.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout), /*playlist.position*/pls);
		if(fragmentList ==null
				|| fragmentList.getAdapter()==null
				|| fragmentList.getAdapter().getAdapterItems()==null
				|| fragmentList.getAdapter().getAdapterItems().isEmpty())
			fragmentDrawer.updateDrawerState(true);
		//fragmentDrawer.selectItem(nowPlaying.getPlaylist().position);

	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if(actionBar!=null){
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
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
			loadingFinished();
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
	public void disconnect() {
		if(mServiceConnection!=null && getService()!=null && getService().isServiceConnected) this.unbindService(mServiceConnection);
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


}


