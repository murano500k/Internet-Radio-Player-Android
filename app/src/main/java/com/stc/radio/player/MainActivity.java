package com.stc.radio.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

import com.activeandroid.query.Select;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.materialize.MaterializeBuilder;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Playlist;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.DialogShower;
import com.stc.radio.player.utils.RequestControlsInteraction;

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
		Timber.d("StationListItem clicked %s", item.station.url);
		KeyboardUtil.hideKeyboard(getActivity());
		bus.post(item);
	}
	@Override
	public void onControlsFragmentInteraction(int value) {
		Timber.d("controls clicked %d", value);
		KeyboardUtil.hideKeyboard(getActivity());
		bus.post(new RequestControlsInteraction(value));
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		Timber.d("position= %d", position);
		loadingStarted();
		Playlist playlist = new Select().from(Playlist.class).where("Position = ?", position).executeSingle();
		if(playlist==null) {
			getService().checkDbContent();
			return;
		}
		loadingFinished();
		assertNotNull("position="+position+" count="+new Select().from(Playlist.class).count(),playlist);
		//fragmentDrawer.selectItem(position);
		Station station = new Select().from(Station.class).where("PlaylistId = ?", playlist.getId()).executeSingle();
		assertNotNull(station);
		NowPlaying nowPlaying=NowPlaying.getInstance();
		nowPlaying.setStation(station);
		onNowPlayingUpdate(nowPlaying);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBufferUpdate(BufferUpdate bufferUpdate) {
		assertNotNull(bufferUpdate);
		progressBar.setIndeterminate(false);
		progressBar.setProgress(bufferUpdate.getAudioBufferSizeMs() * progressBar.getMax() /
				bufferUpdate.getAudioBufferCapacityMs());
	}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onPlaylistUpdate(Playlist playlist) {
		Timber.d("");
		assertNotNull(playlist);
		initDrawerAndUI(playlist);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNowPlayingUpdate(NowPlaying nowPlaying) {
		Timber.w("nowPlaying %d %s %s", nowPlaying.getStatus(),nowPlaying.getStation().name,nowPlaying.getPlaylist().name);

		if(fragmentList == null) initListFragment();

		if(fragmentList.getAdapter().getAdapterItems()==null
				||fragmentList.getAdapter().getAdapterItems().size()==0
				|| fragmentList.getAdapter().getAdapterItems().get(0)==null
				||fragmentList.getAdapter().getAdapterItems().get(0).station.playlistId!=nowPlaying.getPlaylist().getId()){
			if(checkDbSubscription!=null && checkDbSubscription.isUnsubscribed()) checkDbSubscription.unsubscribe();
			updateList(nowPlaying.getActiveList());
			return;
		}
		if(fragmentControls==null) initControlsFragment();
		fragmentControls.updateMetadata(nowPlaying.getMetadata());
		fragmentControls.updateButtons(nowPlaying.getStatus());
		fragmentControls.updateStation(nowPlaying.getStation());

		assertNotNull(fragmentList);
		fragmentList.updateSelection(nowPlaying.getStation());

		progressBar.setIndeterminate((nowPlaying.getStatus()==NowPlaying.STATUS_PLAYING
				|| nowPlaying.getStatus()==NowPlaying.STATUS_IDLE));
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
						//initDrawerAndUI();
						if(!isServiceConnected()) connect();
						showToast("Loading success " + integer);
						Timber.w("Loading success %d" , integer);
						if(checkDbSubscription!=null && !checkDbSubscription.isUnsubscribed()) checkDbSubscription.unsubscribe();
					}
				});
	}

	public void updateList(List<Station> list){
		if(fragmentList==null) initListFragment();
		assertNotNull(fragmentList);
		assertTrue(list!=null && !list.isEmpty());
		FastItemAdapter fastAdapter=fragmentList.getAdapter();
		loadingStarted();
		DbHelper.resetActiveStations();
		for(Station s:list) {
			/*if(s.artPath==null || !new File(s.artPath).exists()) {
				s.artPath=getArtPath(this, getArtName(s.url));
				s.save();
				Target target = new Target() {
					@Override
					public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
						Timber.d("From %s", from.toString());
						insertListItemWithBitmap(s,bitmap,fastAdapter);
						saveArtBitmap(s, bitmap);
					}

					@Override
					public void onBitmapFailed(Drawable errorDrawable) {
						Timber.e("From %s", errorDrawable.toString());
						Bitmap bitmap=drawableToBitmap(errorDrawable);
						insertListItemWithBitmap(s,bitmap,fastAdapter);
						saveArtBitmap(s, bitmap);
					}

					@Override
					public void onPrepareLoad(Drawable placeHolderDrawable) {
					}
				};
				Picasso.with(this).load(s.artPath).error(getDrawable(R.drawable.default_art)).into(target);

			}else*/ insertListItemWithBitmap(s, null/*NowPlaying.getStationArtBitmap(s)*/, fastAdapter);
		}
		loadingFinished();
	}
	public static String getArtName(String url) {
		String pls=null;
		if(url.contains("di.fm")) pls="di";
		else if(url.contains("rock")) pls="rockradio";
		else if(url.contains("jazz")) pls="jazzradio";
		else if(url.contains("tunes")) pls="radiotunes";
		else if(url.contains("classic")) pls="classicalradio";
		else return null;
		String station = url.substring(url.lastIndexOf("/")+1);
		station=station.substring(0, station.indexOf("?"));
		String result = pls+"_"+station;
		if(url.contains("di.fm")) result+=".jpg";
		else result+="_hi.jpg";
		//String basePath=context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+"/";
		//Timber.v(basePath);
		return result;
	}
	public static String getArtPath(Context context, String artName) {
		String basePath=context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+"/";
		Timber.v(basePath);
		return basePath+artName;
	}
	public static  void saveArtBitmap(Station station, Bitmap bitmap){
		Observable.just(bitmap).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(bitmap1 -> {
			try {
				File file = new File(station.artPath);
				if (!file.exists()) {
					assertNotNull(file.createNewFile());
				}
				FileOutputStream ostream = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
				ostream.close();
				Timber.d("artPath for %s =  %s", station.name, station.artPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	public void insertListItemWithBitmap (Station s, Bitmap bitmap,  FastItemAdapter fastAdapter){
			//assertNotNull(bitmap);
			StationListItem stationListItem = new StationListItem().withStation(s).withIcon(bitmap);
			fastAdapter.add(stationListItem);
			stationListItem.station.active = true;
			stationListItem.station.position = fastAdapter.getGlobalPosition(fastAdapter.getAdapterPosition(stationListItem));
			stationListItem.station.save();
			fastAdapter.notifyAdapterItemInserted(stationListItem.station.position);
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
		loadingStarted();
		connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ButterKnife.bind(this);
		new MaterializeBuilder().withActivity(this).build();
		if(!bus.isRegistered(this)) bus.register(this);
		Timber.w("oncreate  will check service");

		if(!isServiceConnected()) {
			Timber.w("oncreate service is NOT connected");
			connect();
		}
		else {
			Timber.w("oncreate service is connected");
			if(NowPlaying.getInstance()==null) {
				Timber.e("NowPlaying=null");

				return;
			}
			Playlist playlist = NowPlaying.getInstance().getPlaylist();
			if(playlist!=null) {
				initDrawerAndUI(playlist);
				updateList(NowPlaying.getInstance().getActiveList());
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().contains(INTENT_OPEN_APP))
			Timber.v("onNewIntent");
		else if (intent.getAction().contains(INTENT_CLOSE_APP)) {
			if(isServiceConnected()) this.unbindService(mServiceConnection);
			finish();
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
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		if(isServiceConnected()) disconnect();
		super.onDestroy();
	}

	private void initListFragment(){
		fragmentList= ListFragment.newInstance();
		assertNotNull(fragmentList);
		FragmentManager fragmentManager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.container_list, fragmentList);
		ft.commit();
	}

	private void initControlsFragment() {
		Timber.d("initControlsFragment");
		fragmentControls = PlaybackControlsFragment.newInstance();
		assertNotNull(fragmentControls);
		FragmentManager fragmentManager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.container_controls, fragmentControls);
		ft.commit();
	}


	public void initDrawerAndUI(Playlist playlist) {
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		dialogShower = new DialogShower();
		fragmentDrawer = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		if (fragmentDrawer == null) {
			throw new IllegalStateException("Mising fragment with id 'fragment_naivgation_drawer'. Cannot continue.");
		}
		assertNotNull(playlist);

		fragmentDrawer.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout), NowPlaying.getInstance().getPlaylist().position);
		fragmentDrawer.selectItem(NowPlaying.getInstance().getPlaylist().position);
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
		setTitle(NowPlaying.getInstance().getPlaylist().name);
		restoreActionBar();
		if(splashScreen==null) splashScreen= (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.GONE);
	}


	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if(actionBar!=null){
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(NowPlaying.getInstance().getPlaylist().name);
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
			serviceRadioRx.isServiceConnected=true;
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

	public static Bitmap drawableToBitmap(Drawable drawable) {
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


