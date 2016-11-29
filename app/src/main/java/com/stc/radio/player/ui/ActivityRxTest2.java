package com.stc.radio.player.ui;

public class ActivityRxTest2 {}
		/*
		extends AppCompatActivity {
	public static final String TAG = "ActivityRadio";
	public static final String INTENT_TOKEN_UPDATED = "com.stc.radio.player.INTENT_TOKEN_UPDATED";
	public static final String INTENT_UPDATE_STATIONS = "com.stc.radio.player.INTENT_UPDATE_STATIONS";
	public static final String EXTRA_UPDATE_STATIONS_RESULT_STRING = "com.stc.radio.player.EXTRA_UPDATE_STATIONS_RESULT_STRING";
	public static final String EXTRA_UPDATE_STATIONS_INDEX = "com.stc.radio.player.EXTRA_UPDATE_STATIONS_INDEX";
	public static final String INTENT_OPEN_APP = "com.stc.radio.player.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.stc.radio.player.INTENT_CLOSE_APP";
	private static final int SELECT_PLS_REQUEST_CODE = 444;
	private Subscription internetListener;

	final public static class UI_STATE{

		static final int LOADING= 0;
		static final int PLAYING= 1;
		static final int IDLE= -1;
	}
	Drawer drawer;
	public static final int INDEX_SHUFFLE=7;
	static final int INDEX_SLEEP=8;
	public static final int INDEX_RESET=9;
	public static final int INDEX_BUFFER=10;

	RecyclerView recyclerView;
	ListAdapterRx adapter;
	ImageButton btnPlay, btnPrev, btnNext;
	private AVLoadingIndicatorView spinner;
	private static ServiceRadioRx serviceRadioRx;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private UrlManager urlManager;


	public PlaylistManager getPlaylistManager() {
		return playlistManager;
	}

	public DialogShower getDialogShower() {
		return dialogShower;
	}

	private PlaylistManager playlistManager;
	private LinearLayout loadingLayout;
	private Toast toast;
	private PlaylistDownloader playlistDownloader;
	private DrawerManager drawerManager;
	Toolbar toolbar;
	private boolean isPlaying() {
		return serviceRadioRx != null && serviceRadioRx.isPlaying();
	}
	public ServiceRadioRx getService() {
		return serviceRadioRx;
	}
	public boolean isServiceConnected() {
		return (serviceRadioRx!=null && serviceRadioRx.isServiceConnected);
	}

	public ConnectivityManager getConnectivityManager() {
		return connectivityManager;
	}

	public ConnectivityManager connectivityManager;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			Log.i(TAG, "Service Connected.");
			serviceRadioRx = ((ServiceRadioRx.LocalBinder) binder).getService();
			updateSleepDrawerItem(serviceRadioRx.isSleepTimerRunning());
			serviceRadioRx.isServiceConnected=true;
			loadingFinished();
		}

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(isTablet()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		setContentView(R.layout.activity_rx_test);
		setSupportActionBar(toolbar);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if(getSupportActionBar()!=null)getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		EventBus.getDefault().register(this);
		playlistManager = new PlaylistManager(getApplicationContext());
		playlistDownloader=new PlaylistDownloader(getApplicationContext());
		initDrawerAndUI();
		loadingListStarted();
		urlManager =new UrlManager(this);
		initDrawer(savedInstanceState);
		initList(new ArrayList<>(), playlistManager.getSelectedUrl());
		if(initActivePlaylist()){
			initList(playlistManager.getStations(), playlistManager.getSelectedUrl());
			if(!isServiceConnected()) connect();
			else loadingFinished();
		}
	}
	public void initDrawer(Bundle savedInstanceState){
		Log.d(TAG, "initDrawer");
		drawerManager=new DrawerManager();

		toolbar.setOnClickListener(v -> openDrawer());
		DrawerBuilder drawerBuilder = new DrawerBuilder()
				.withActivity(this)
				.withToolbar(toolbar)
				.withDisplayBelowStatusBar(false)
				.withActionBarDrawerToggleAnimated(true)
				.withSliderBackgroundColorRes(R.color.colorSecondary)
				.addDrawerItems(
						new SectionDrawerItem().withDivider(true),
						//drawerManager.getPlaylistDrawerItem(this, R.string.title_section_di, INDEX_DI),
						//drawerManager.getPlaylistDrawerItem(this, R.string.title_section_radiotunes, INDEX_RT),
						//drawerManager.getPlaylistDrawerItem(this, R.string.title_section_jazz, INDEX_JR),
						//drawerManager.getPlaylistDrawerItem(this, R.string.title_section_rock, INDEX_RR),
						//drawerManager.getPlaylistDrawerItem(this, R.string.title_section_classic, INDEX_CR),
						//drawerManager.getPlaylistDrawerItem(this,R.string.title_section_custom,INDEX_CUSTOM),
						new DividerDrawerItem()
						//drawerManager.getDrawerItemShuffle(this, playlistManager.isShuffle(), INDEX_SHUFFLE),
						//drawerManager.getDrawerItemBuffer(this, INDEX_BUFFER),
						//drawerManager.getDrawerItemReset(this, INDEX_RESET)
				)
				.withSavedInstance(savedInstanceState)
				.withHasStableIds(true);
		if (isTablet()) {
			this.drawer = drawerBuilder.buildView();
			((ViewGroup) findViewById(R.id.nav_tablet)).addView(drawer.getSlider());
		} else this.drawer = drawerBuilder.build();
	}
	public boolean updateSleepDrawerItem(boolean isSleepActive){
		if(drawer!=null && drawer.getDrawerItem(INDEX_SLEEP)==null) {
			//drawer.addItemAtPosition(
			//		drawerManager.getDrawerItemSleep(this,
			//				isSleepActive, INDEX_SLEEP),INDEX_SLEEP);
			return true;
		}else return false;
	}
	@Override
	public void onBackPressed() {
		//handle the back press :D close the drawer first and if the drawer is closed close the activity
		if (drawer != null && drawer.isDrawerOpen()) {
			drawer.closeDrawer();
		} else {
			super.onBackPressed();
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
					Log.w(TAG, "network connected");
					showToast("network connected");
					int i = playlistManager.getActivePlaylistIndex();
					playlistDownloader.downloadPlaylist(urlManager.getPlaylistUrl(i),i);
					internetListener.unsubscribe();
					if(!isServiceConnected())connect();
					//loadingFinished();
				}
				});
	}
	public boolean checkNetwork(){
		if(!isNetworkConnected()){
			showToast("No connection");
				if(internetListener==null || internetListener.isUnsubscribed())
					internetListener =getInternetListener("https://google.com/");
			loadingListStarted();

			return false;
		}
		return true;
	}

	private boolean initActivePlaylist(){
		if(playlistManager.getStations()==null){
			if(!checkNetwork()) return false;
			int i = playlistManager.getActivePlaylistIndex();
			String url=null;
			if(i==INDEX_CUSTOM) {
				String res = urlManager.getSavedCustomPlaylistString();
				if (res != null) {
					Intent intent = new Intent(this, ActivityRxTest2.class);
					intent.setAction(ActivityRxTest2.INTENT_UPDATE_STATIONS);
					intent.putExtra(ActivityRxTest2.EXTRA_UPDATE_STATIONS_RESULT_STRING, res);
					intent.putExtra(ActivityRxTest2.EXTRA_UPDATE_STATIONS_INDEX, i);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					return false;
				}
			} else url = urlManager.getPlaylistUrl(i);
			if (url == null || url.length() == 0 || url.contains("undefined")) {
				i = INDEX_DI;
				playlistManager.saveActivePlaylistIndex(i);
				url = urlManager.getPlaylistUrl(i);
			}
			playlistDownloader.downloadPlaylist(url,i);
			return false;
		}else {
			return true;
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState = drawer.saveInstanceState(outState);
		if(drawer!=null	&& drawer.getCurrentSelection()>0){
			Long i = drawer.getCurrentSelection();
			playlistManager.saveActivePlaylistIndex(i.intValue());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(intent.getAction().contains(INTENT_OPEN_APP)) updateControls();
		else if(intent.getAction().contains(INTENT_CLOSE_APP)){
			this.unbindService(mServiceConnection);
			finish();
		}
		else if(intent.getAction().contains(INTENT_UPDATE_STATIONS)){
			int i = intent.getIntExtra(EXTRA_UPDATE_STATIONS_INDEX, -1);
			String s=intent.getStringExtra(EXTRA_UPDATE_STATIONS_RESULT_STRING);
			Log.w("onUpdateStations", "index="+i);
			Log.w("onUpdateStations", "str_result="+s);
			playlistManager.saveActivePlaylistIndex(i);
			if(i==INDEX_CUSTOM) urlManager.saveCustomPlaylistString(s);
			initList(playlistManager.selectPls(s), playlistManager.getSelectedUrl());
			if(isServiceConnected()) {
				Intent intentReset=new Intent(this,ServiceRadioRx.class);
				intentReset.setAction(ServiceRadioRx.INTENT_RESET);
				startService(intentReset);
				loadingFinished();
			} else connect();
		}
		super.onNewIntent(intent);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==RESULT_CANCELED) {
			drawer.setSelection(INDEX_DI);
			Toast.makeText(getApplicationContext(), "Playlist not selected", Toast.LENGTH_SHORT).show();
		}
		else if(resultCode==RESULT_OK){
			if(requestCode==SELECT_PLS_REQUEST_CODE) {
				playlistDownloader.downloadPlaylist(data.getData().toString(), INDEX_CUSTOM);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();


	}

	public void selectNewPlaylist(int i) {
		Log.v(TAG, "selectNewPlaylist "+ i);
		if(!isServiceConnected()) {
			showToast("service not connected");
			return;
		}
		if(i==INDEX_CUSTOM) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("audio/mpeg-url");
			intent.setType("audio/x-mpegurl");
			intent.setType("audio/mpegurl");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			try {
				startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), SELECT_PLS_REQUEST_CODE);
			} catch (android.content.ActivityNotFoundException ex) {
				// Potentially direct the user to the Market with a Dialog
				Toast.makeText(getApplicationContext(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
			}
		}else playlistDownloader.downloadPlaylist(
				urlManager.getPlaylistUrl(i), i);
		loadingListStarted();
	}
	private void loadingListStarted(){
		if(EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().unregister(this);
		}
		if(loadingLayout==null) loadingLayout =(LinearLayout) findViewById(R.id.select_pls);
		closeDrawer();
		loadingLayout.setVisibility(View.VISIBLE);
		updateControls(UI_STATE.LOADING, "loading playlist",0);
	}
	public void loadingFinished(){
		if(!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
		}
		if(loadingLayout==null) loadingLayout =(LinearLayout) findViewById(R.id.select_pls);
		loadingLayout.setVisibility(View.GONE);
		closeDrawer();
		updateControls();
		setTitle(playlistManager.getActivePlaylistName());
		if(!(isServiceConnected() && serviceRadioRx.isSleepTimerRunning()))
			updateSleepDrawerItem(false);
	}
public void closeDrawer(){
	if(drawer!=null && drawer.isDrawerOpen()) drawer.closeDrawer();
	if(getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
}
	public void openDrawer(){
		if(drawer!=null && !drawer.isDrawerOpen()) drawer.openDrawer();
		if(getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private void btnControlClick(String extra){
		Log.i(TAG, "btnControlClick: "+extra);
		if(isServiceConnected()) {
			EventBus.getDefault().post(new UiEvent(UiEvent.UI_ACTION.LOADING_STARTED, ServiceRadioRx.STATUS_LOADING, playlistManager.getSelectedUrl()));
			Intent intent = new Intent(getApplicationContext(), ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(extra, extra);
			startService(intent);
		}else Toast.makeText(getApplicationContext(),"Player is locked!", Toast.LENGTH_SHORT).show();
	}
	public void initDrawerAndUI() {

		progressBar=(ProgressBar) findViewById(R.id.progressBar);
		dialogShower =new DialogShower(playlistManager);

		btnNext = (ImageButton) findViewById(R.id.btnNext);
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

	}

	public void initList(ArrayList<String> list,String urlSelected) {
			adapter = new ListAdapterRx(getApplicationContext(), list);
			adapter.setUrlSelected(urlSelected);
			adapter.setHasStableIds(false);
		//adapter.setValues(list, list.get(0));
			recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));
			recyclerView.setAdapter(adapter);
			recyclerView.setItemAnimator(new AlphaCrossFadeAnimator());
		if (list.size() != 0) adapter.notifyDataSetChanged();
	}

	public void updateControls(){
		updateControls(isPlaying() ? UI_STATE.PLAYING : UI_STATE.IDLE,  playlistManager.getActivePlaylistName(), 100);
	}

	@Override
	public void setTitle(CharSequence title) {
		toolbar.setTitle(title);
		super.setTitle(title);
	}

	private void updateControls(int state, String title, int progress) {
		switch (state) {
			case UI_STATE.LOADING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(true);
				spinner.setVisibility(View.VISIBLE);
				spinner.animate();
				//btnPlay.setVisibility(View.INVISIBLE);
				btnPlay.setActivated(true);
				break;
			case UI_STATE.PLAYING:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(false);
				spinner.setVisibility(View.GONE);
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(true);
				break;
			case UI_STATE.IDLE:
				setTitle(title);
				progressBar.setProgress(progress);
				progressBar.setIndeterminate(false);
				spinner.setVisibility(View.GONE);
				btnPlay.setVisibility(View.VISIBLE);
				btnPlay.setActivated(false);
				break;
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiEvent(UiEvent eventUi) {
		UiEvent.UI_ACTION action = eventUi.getUiAction();
		String url = eventUi.getExtras().url;
		String name = "";
		if(url!=null && !url.equals("")) name=playlistManager.getActivePlaylistName();

		if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)) {
			Log.i(TAG, "onPlayerStartedEvent: " + name);
			updateControls(UI_STATE.PLAYING, name, 0);
			if (recyclerView != null
					&& adapter != null
					&& adapter.getItemIndex(eventUi.getExtras().url)
					!= -1)
				recyclerView.smoothScrollToPosition(adapter.getItemIndex(eventUi.getExtras().url));
		}
		else if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)) {
			updateControls(UI_STATE.IDLE, playlistManager.getActivePlaylistName(), 0);
		}
		else if (action.equals(UiEvent.UI_ACTION.BUFFER_UPDATED)) {
			updateControls(UI_STATE.PLAYING, name, eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.LOADING_STARTED)) {
			updateControls(UI_STATE.LOADING, "loading...", eventUi.getExtras().progress);
		}
		else if (action.equals(UiEvent.UI_ACTION.STATION_SELECTED)) {
			Log.i(TAG, "onStationSelected: "+ name);
			updateControls(UI_STATE.LOADING, name, 0);
			//if(recyclerView!=null && adapter!=null && adapter.getItemIndex(eventUi.getExtras().url)!=-1)
			//	recyclerView.smoothScrollToPosition(adapter.getItemIndex(eventUi.getExtras().url));
		}
	}


	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onSleepTimerStatusUpdate(SleepEvent sleepEvent) {
		SleepEvent.SLEEP_ACTION action=sleepEvent.getSleepAction();
		int seconds=sleepEvent.getSeconds();
		Log.d(TAG, "onSleepTimerStatusUpdate " + action + " " + seconds);
		if (action== SleepEvent.SLEEP_ACTION.CANCEL) {
			Toast.makeText(getApplicationContext(), "Sleep time cancelled", Toast.LENGTH_SHORT).show();
				//drawer.updateItem(drawerManager.getDrawerItemSleep(this,false, INDEX_SLEEP));
			setTitle(R.string.app_name);
		} else if (action== SleepEvent.SLEEP_ACTION.UPDATE) {
			if (seconds > 1) {
				if(seconds>60) setTitle(seconds/60 + " minutes left");
				else setTitle(seconds+" seconds to sleep");
				*//*drawer.updateItem(
						drawerManager.getDrawerItemSleep(this,true, INDEX_SLEEP));*//*

			}
		} else if (action== SleepEvent.SLEEP_ACTION.FINISH) {
			Toast.makeText(getApplicationContext(), "Sleep timer finished", Toast.LENGTH_SHORT).show();
			//drawer.updateItem(
			//		drawerManager.getDrawerItemSleep(this,false, INDEX_SLEEP));
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
	public boolean isTablet() {
		return (checkDimension(this)>=7);
	}
	private static double checkDimension(Context context) {

		WindowManager windowManager = ((Activity)context).getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);

		// since SDK_INT = 1;
		int mWidthPixels = displayMetrics.widthPixels;
		int mHeightPixels = displayMetrics.heightPixels;

		// includes window decorations (statusbar bar/menu bar)
		try
		{
			Point realSize = new Point();
			Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
			mWidthPixels = realSize.x;
			mHeightPixels = realSize.y;
		}
		catch (Exception ignored) {}

		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(mWidthPixels/dm.xdpi,2);
		double y = Math.pow(mHeightPixels/dm.ydpi,2);
		double screenInches = Math.sqrt(x+y);
		Log.d("debug","Screen inches : " + screenInches);
		return screenInches;
	}
	public int getScreenOrientation( ) {
		Display screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		int orientation;
		if(screenOrientation.getWidth() <=
				screenOrientation.getHeight())
			orientation = Configuration.ORIENTATION_PORTRAIT;
		else
			orientation = Configuration.ORIENTATION_LANDSCAPE;
		return orientation;
	}
	public PlaylistDownloader getPlaylistDownloader() {
		return playlistDownloader;
	}
}
*/