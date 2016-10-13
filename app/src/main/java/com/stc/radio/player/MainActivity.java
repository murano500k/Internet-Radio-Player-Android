package com.stc.radio.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.materialize.MaterializeBuilder;
import com.mikepenz.materialize.util.KeyboardUtil;
import com.stc.radio.player.contentmodel.PlaylistContent;
import com.stc.radio.player.contentmodel.Retro;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.PLAYLISTS;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.ui.BufferUpdate;
import com.stc.radio.player.ui.DialogShower;
import com.stc.radio.player.utils.RequestControlsInteraction;
import com.stc.radio.player.utils.SettingsProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;

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

	rx.Observer<List<Station>> listUpdateObserver;
	rx.Subscription listUpdateSubscription;

	@Override
	public void onListFragmentInteraction(StationListItem item) {
		assertNotNull(item);
		Timber.d("StationListItem clicked %s", item.station.getUrl());
		KeyboardUtil.hideKeyboard(getActivity());
		bus.post(item.station);
	}
	@Override
	public void onControlsFragmentInteraction(int value) {
		Timber.d("controls clicked %d", value);
		KeyboardUtil.hideKeyboard(getActivity());
		bus.post(new RequestControlsInteraction(value));
	}
	public rx.Observer<List<Station>> getListObserver(){
		return new rx.Observer<List<Station>>() {

			@Override
			public void onCompleted() {
				From from =new Select().from(Station.class);
				if(from.exists()){
					List<Station> list= from.execute();
					int i=0;

					for(Station station : list) {
						if(i==0) {
							if(nowPlaying==null)nowPlaying=new NowPlaying();
							nowPlaying.setStation(station);
							nowPlaying.save();						}
							Timber.d("station%d = %s", i++, station.toString());
					}
				}

				runOnUiThread(() -> {
					initControlsFragment();
					checkService();

				});

				Timber.w("SUCCESS");
			}

			@Override
			public void onError(Throwable e) {
				Timber.e(e,"FAIL");

				new AlertDialog.Builder(getApplicationContext()).setTitle("ERROR download stations failed").setMessage("Please, retry").setPositiveButton("Retry", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Station station = NowPlaying.getInstance().getStation();
						assertNotNull(station);
						listUpdateSubscription=observePlsUpdate(station.getPlaylist());
					}
				});
			}

			@Override
			public void onNext(List<Station> stations) {
				stations.get(0).save();

				showToast("Playlist " + stations.get(0).getPlaylist() + " loaded successfully, " + stations.size() + " stations added");
			}
		};
	}
	public Subscription observePlsUpdate(String pls){
		if (listUpdateObserver == null) {
			listUpdateObserver = getListObserver();
		}
		loadingStarted();
		return Observable.just(Retro.hasValidToken()).observeOn(Schedulers.newThread()).subscribeOn(Schedulers.newThread()).flatMap(new Func1<Boolean, Observable<String>>() {
			@Override
			public Observable<String> call(Boolean aBoolean) {
				if(aBoolean) return Observable.just(SettingsProvider.getToken());
				else  return Observable.just(Retro.updateToken());
			}
		}).flatMap(new Func1<String, Observable<String>>() {
			@Override
			public Observable<String> call(String s) {
				Timber.w("Token = %s", s);
				return Observable.just(pls);
			}
		}).flatMap(new Func1<String, Observable<List<Station>>>() {
			@Override
			public Observable<List<Station>> call(String s) {
				//fragmentDrawer.updateDrawerState(false);

				From from = new Select().from(Station.class).where("Playlist = ?", s);
				if (from.exists()) {
					List<Station> list = from.execute();
					Timber.w("db list size %d",list.size());
					if (list.size() > 1) return Observable.just(list);
				}
				throw new RuntimeException("ERROR Stations not found");
			}
		}).onErrorResumeNext(new Func1<Throwable, Observable<? extends List<Station>>>() {
			@Override
			public Observable<? extends List<Station>> call(Throwable throwable) {
				if (throwable.getMessage().contains("ERROR")) {
					Call<List<PlaylistContent>> loadSizeCall = Retro.getStationsCall(pls);
					Response<List<PlaylistContent>> response = null;
					try {
						response = loadSizeCall.execute();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					if (response != null && response.isSuccessful() && !response.body().isEmpty()) {
						List<Station> stations = new ArrayList<Station>();
						for (PlaylistContent playlistContent : response.body())
							stations.add(new Station(playlistContent, pls));
						ActiveAndroid.beginTransaction();
						try {
							for (Station station : stations) station.save();
							ActiveAndroid.setTransactionSuccessful();
						} catch (Exception e) {
							throw new RuntimeException(e);
						} finally {
							ActiveAndroid.endTransaction();
						}
						return Observable.just(stations);
					}
					else Timber.e("request pls error");
				}
				throw new RuntimeException("ERROR download pls failed");
			}
		}).flatMap(new Func1<List<Station>, Observable<Station>>() {
			@Override
			public Observable<Station> call(List<Station> stations) {
				if (stations != null && !stations.isEmpty()) {
					Timber.w("downloaded pls size %d",stations.size());
					nowPlaying=NowPlaying.getInstance();
					nowPlaying.setStation(stations.get(0), false);
					nowPlaying.save();
					DbHelper.resetActiveStations();

					runOnUiThread(() -> {
						if (fragmentList == null) initListFragment();
						FastItemAdapter<StationListItem> adapter = fragmentList.getAdapter();
						adapter.clear();
						adapter.notifyAdapterDataSetChanged();
					});
				} else throw new RuntimeException("ERROR no stations in list");
				return Observable.from(stations);
			}
		}).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.computation()).delay(1000, TimeUnit.MILLISECONDS).flatMap(new Func1<Station, Observable<StationListItem>>() {
			@Override
			public Observable<StationListItem> call(Station station) {
				StationListItem listItem = new StationListItem().withStation(station);
				return Observable.just(listItem);
			}
		}).doOnNext(new Action1<StationListItem>() {
			@Override
			public void call(StationListItem stationListItem) {

				Timber.w("onAdapterAddItem %s",stationListItem.getStation().getName());
				runOnUiThread(() -> {
					FastItemAdapter<StationListItem> adapter = fragmentList.getAdapter();
					adapter.add(stationListItem);
					stationListItem.station.setActive(true);
					int pos = adapter.getAdapterPosition(stationListItem);
					stationListItem.station.setPosition(pos);
					adapter.notifyAdapterItemInserted(pos);
				});
			}
		}).toList().flatMap(new Func1<List<StationListItem>, Observable<List<Station>>>() {
			@Override
			public Observable<List<Station>> call(List<StationListItem> stationListItems) {
				List<Station> stations = new ArrayList<Station>();
				for (StationListItem stationListItem : stationListItems)
					stations.add(stationListItem.station);
				return Observable.just(stations);
			}
		}).doOnNext(new Action1<List<Station>>() {
			@Override
			public void call(List<Station> stations) {
				ActiveAndroid.beginTransaction();
				try {
					for (Station station : stations) station.save();
					ActiveAndroid.setTransactionSuccessful();
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					ActiveAndroid.endTransaction();
				}
			}
		}).subscribe(listUpdateObserver);
	}

	@Override
	public void onNavigationDrawerItemSelected(String pls) {
		Timber.d("onNavigationDrawerItemSelected pls= %s", pls);
		if(listUpdateSubscription!=null && !listUpdateSubscription.isUnsubscribed()) {
			showToast("Please wait");
			return;
		}else if(pls==null) {
			showToast("Please wait");
			Timber.e(pls);
		}
		listUpdateSubscription=observePlsUpdate(pls);
		return;
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBufferUpdate(BufferUpdate bufferUpdate) {
		assertNotNull(bufferUpdate);
		progressBar.setIndeterminate(false);
		progressBar.setProgress(bufferUpdate.getAudioBufferSizeMs() * progressBar.getMax() /
				bufferUpdate.getAudioBufferCapacityMs());
	}


	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNowPlayingUpdate(NowPlaying updatedNowPlaying) {
		/*if(this.nowPlaying==null || !this.nowPlaying.equals(updatedNowPlaying)) {*/
			this.nowPlaying=updatedNowPlaying;



			if (fragmentControls == null) initControlsFragment();
			else {
				fragmentControls.updateMetadata(nowPlaying.getMetadata());
				fragmentControls.updateButtons(nowPlaying.getStatus());
				fragmentControls.updateStation(nowPlaying.getStation());
			}
			assertNotNull(fragmentList);
			fragmentList.updateSelection(nowPlaying.getStation());
			progressBar.setIndeterminate((nowPlaying.getStatus() == NowPlaying.STATUS_PLAYING
					|| nowPlaying.getStatus() == NowPlaying.STATUS_IDLE));
	//	}
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



		Timber.w("active pls %s", nowPlaying.getPlaylist());
		initDrawer();
		initListFragment();
		initControlsFragment();
		SettingsProvider.setDbExistsTrue();
		if(!bus.isRegistered(this))bus.register(this);
		restoreActionBar();
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
		ButterKnife.bind(this);
		new MaterializeBuilder().withActivity(this).build();
		initDrawer();
		initListFragment();
		if(!initNowPlaying()) {
			Timber.w("DB empty. Now will download");
			listUpdateSubscription=observePlsUpdate(PLAYLISTS.DI);
			return;
		}
		if(fragmentList.getAdapter()!=null && fragmentList.getAdapter().getAdapterItemCount()<=0){

			String openedPls=nowPlaying.getStation().getPlaylist();
			Timber.w("list empty. Now will update pls: %s", openedPls);

			listUpdateSubscription=observePlsUpdate(openedPls);
			return;
		}
		checkService();
	}
	private boolean initNowPlaying(){
		nowPlaying=NowPlaying.getInstance();
		Station station = nowPlaying.getStation();
		return station!=null;
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
		//if(!bus.isRegistered(this))bus.register(this);
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
			DbHelper.resetActiveStations();
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


	public void initDrawer() {
		Timber.w("init drawer pls");
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
				(DrawerLayout) findViewById(R.id.drawer_layout), /*playlist.position*/0);

		//fragmentDrawer.selectItem(nowPlaying.getPlaylist().position);

	}
	private void loadingStarted(){
		showToast("Loading started");
		//Timber.w("Loading started");
		if(bus.isRegistered(this)) bus.unregister(this);

		if(splashScreen==null) splashScreen= (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.VISIBLE);
		setTitle("LOADING...");
	}
	public void loadingFinished(){
		if(nowPlaying!=null && nowPlaying.getStation()!=null && nowPlaying.getStation().getPlaylist()!=null) setTitle(nowPlaying.getStation().getPlaylist());
		hideToast();
		restoreActionBar();
		if(splashScreen==null) splashScreen= (ProgressBar) findViewById(R.id.progress_splash);
		if(splashScreen!=null) splashScreen.setVisibility(View.GONE);
		if(!bus.isRegistered(this)) bus.register(this);
	}


	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if(actionBar!=null){
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(nowPlaying.getPlaylist());
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


