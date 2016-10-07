package com.stc.radio.player.ui;

public class ActivityRxTest
		/*extends AppCompatActivity implements ItemAdapter.ItemFilterListener, ItemTouchCallback*/

{
	private PlaybackControlsFragment mControlsFragment;

	public static final String TAG = "ActivityRadio";
	public static final String INTENT_PLAY_NEXT = "com.stc.radio.player.INTENT_PLAY_NEXT";
	public static final String INTENT_PLAY_PREV = "com.stc.radio.player.INTENT_PLAY_PREV";
	public static final String INTENT_TOKEN_UPDATED = "com.stc.radio.player.INTENT_TOKEN_UPDATED";
	public static final String INTENT_UPDATE_STATIONS = "com.stc.radio.player.INTENT_UPDATE_STATIONS";
	public static final String EXTRA_UPDATE_STATIONS_RESULT_STRING = "com.stc.radio.player.EXTRA_UPDATE_STATIONS_RESULT_STRING";
	public static final String EXTRA_UPDATE_STATIONS_INDEX = "com.stc.radio.player.EXTRA_UPDATE_STATIONS_INDEX";
	public static final String INTENT_OPEN_APP = "com.stc.radio.player.INTENT_OPEN_APP";
	public static final String INTENT_CLOSE_APP = "com.stc.radio.player.INTENT_CLOSE_APP";
	private static final int SELECT_PLS_REQUEST_CODE = 444;
	public static final String RADIO_PREFERENCES = "com.stc.radio.player.RADIO_PREFERENCES";
	public static final String SELECTED_PLAYLIST_ID = "com.stc.radio.player.SELECTED_PLAYLIST_POSITION";
	public static final String SELECTED_LIST_POSITION = "com.stc.radio.player.SELECTED_LIST_POSITION";
/*
	private Subscription internetListener;
	private Subscription mSubscription;
	private TextView infoProgress;




	Drawer drawer;
	public static final int INDEX_SHUFFLE=7;
	static final int INDEX_SLEEP=8;
	public static final int INDEX_RESET=9;
	public static final int INDEX_BUFFER=10;

	RecyclerView recyclerView;
	ImageButton btnPlay, btnPrev, btnNext;
	private AVLoadingIndicatorView spinner;
	private static ServiceRadioRx serviceRadioRx;
	private DialogShower dialogShower;
	private ProgressBar progressBar;
	private FastItemAdapter<StationListItem> fastItemAdapter;
	private EventBus bus=EventBus.getDefault();
	private SimpleDragCallback touchCallback;
	private ItemTouchHelper touchHelper;


	public DialogShower getDialogShower() {
		return dialogShower;
	}

	//private LinearLayout loadingLayout;
	private Toast toast;
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
			if(getSelectedItem()!=null)	{
				serviceRadioRx.updateCurrentUrl(getSelectedItem().station.url);

			}

			loadingFinished();
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(isTablet()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		setContentView(R.layout.activity_main);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		//toolbar.setTitleTextColor(getResources().getColor(R.color.colorText));
		//toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
		//if(getSupportActionBar()!=null)getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		EventBus.getDefault().register(this);
		ButterKnife.bind(this);
		new MaterializeBuilder().withActivity(this).build();
		initDrawerAndUI();
		loadingListStarted();
		mSubscription = Observable.just(1)
				.doOnNext(i -> {
							showToast("Loading started");
							Timber.w("Loading started");
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
						drawer=initDrawer(savedInstanceState);
						long plsId=DbHelper.getPlaylistId(getString(R.string.title_section_di));
						if(drawer!=null && drawer.getCurrentSelection()>=0)  plsId=drawer.getCurrentSelection();
						setTitle(DbHelper.getPlaylistName(plsId));
						initFastAdapter(savedInstanceState,plsId);
						if(!isServiceConnected()) connect();
						loadingFinished();
						showToast("Loading success " + integer);
						Timber.w("Loading success %d" , integer);
						if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
					}
				});

	}



	@Override
	protected void onStop() {
		if(mSubscription!=null && !mSubscription.isUnsubscribed()) mSubscription.unsubscribe();
		super.onStop();
	}

	private boolean initFastAdapter(Bundle savedInstanceState, long activePlaylistId){
		fastItemAdapter = new FastItemAdapter<>();
		fastItemAdapter.withSelectable(true);
		fastItemAdapter.withMultiSelect(false);
*//*
		fastItemAdapter.withPositionBasedStateManagement(false);
*//*
		final FastScrollIndicatorAdapter<StationListItem> fastScrollIndicatorAdapter = new FastScrollIndicatorAdapter<>();
		fastItemAdapter.withOnClickListener(listItemOnClickListener);
		fastItemAdapter.withFilterPredicate((item, constraint) -> !item.station.name.toLowerCase()
				.contains(constraint.toString().toLowerCase()));
		fastItemAdapter.getItemAdapter().withItemFilterListener(this);
		Timber.w("PlaylistId = %d", activePlaylistId);
		List<Station> list = new Select()
				.from(Station.class).where("PlaylistId = ?",activePlaylistId).execute();
		assertNotNull(list);
		assertTrue(list.size()>0);
		for(Station s: list) {
			StationListItem stationListItem = new StationListItem()
					.withIdentifier(s.getId())
					.withStation(s);
			fastItemAdapter.add(stationListItem);
		}
		if(savedInstanceState!=null) fastItemAdapter.withSavedInstanceState(savedInstanceState);

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setAdapter(fastScrollIndicatorAdapter.wrap(fastItemAdapter));
		DragScrollBar materialScrollBar = new DragScrollBar(this, recyclerView, true);
		materialScrollBar.addIndicator(new AlphabetIndicator(this), true);
		touchCallback = new SimpleDragCallback(this);
		touchHelper = new ItemTouchHelper(touchCallback);
		touchHelper.attachToRecyclerView(recyclerView);
		*//*if(getActionBar()!=null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(false);
		}*//*
		return true;
	}

	public StationListItem getSelectedItem(){
		Iterator<StationListItem> itemIterator =fastItemAdapter.getSelectedItems().iterator();
		if(itemIterator.hasNext()){
			return itemIterator.next();
		}else {
			//showToast("Station not selected");
			Timber.e("ERROR no selected item");
			return null;
		}
	}
	public int getItemPosition(StationListItem item){
		return fastItemAdapter.getAdapterPosition(item);
	}
	public int getItemPosition(String url){
		for(StationListItem stationListItem: fastItemAdapter.getAdapterItems()){
			if(stationListItem.station.url.contains(url)) {
				return fastItemAdapter.getAdapterPosition(stationListItem);
			}
		}
		return -1;
	}
	public StationListItem getItemFromUrl(String url){
		for(StationListItem stationListItem: fastItemAdapter.getAdapterItems()){
			if(stationListItem.station.url.contains(url)) {
				return stationListItem;
			}
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);

		//search icon
		menu.findItem(R.id.search).setIcon(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_search).color(Color.LTGRAY).actionBar());

			final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();

			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String s) {
					touchCallback.setIsDragEnabled(false);
					fastItemAdapter.filter(s);
					return true;
				}
				@Override
				public boolean onQueryTextChange(String s) {
					fastItemAdapter.filter(s);
					touchCallback.setIsDragEnabled(TextUtils.isEmpty(s));
					return true;
				}
			});
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public void itemsFiltered() {
		Timber.v("filtered items count: %d", fastItemAdapter.getItemCount() );
		Toast.makeText(ActivityRxTest.this, "filtered items count: " + fastItemAdapter.getItemCount(), Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean itemTouchOnMove(int oldPosition, int newPosition) {
		Collections.swap(fastItemAdapter.getAdapterItems(), oldPosition, newPosition); // change position
		fastItemAdapter.notifyAdapterItemMoved(oldPosition, newPosition);
		return true;
	}

	public Drawer initDrawer(Bundle savedInstanceState){
		Log.d(TAG, "initDrawer");
		drawerManager=new DrawerManager();
		toolbar.setOnClickListener(v -> openDrawer());
		DrawerBuilder drawerBuilder = new DrawerBuilder()
				.withActivity(this)
				.withRootView(R.id.drawer_layout)
				.withToolbar(toolbar)
				.withActionBarDrawerToggle(true)
				.withActionBarDrawerToggleAnimated(true)
				.withSliderBackgroundColorRes(R.color.colorPrimary)
				.addDrawerItems(
						new SectionDrawerItem().withDivider(true),
						drawerManager.getPlaylistDrawerItem(this, R.string.title_section_di,
								DbHelper.getPlaylistId(getString(R.string.title_section_di))),

						drawerManager.getPlaylistDrawerItem(this, R.string.title_section_radiotunes,
								DbHelper.getPlaylistId(getString(R.string.title_section_radiotunes))),

						drawerManager.getPlaylistDrawerItem(this, R.string.title_section_jazz,
								DbHelper.getPlaylistId(getString(R.string.title_section_jazz))),

						drawerManager.getPlaylistDrawerItem(this, R.string.title_section_rock,
								DbHelper.getPlaylistId(getString(R.string.title_section_rock))),

						drawerManager.getPlaylistDrawerItem(this, R.string.title_section_classic,
								DbHelper.getPlaylistId(getString(R.string.title_section_classic))),

						new DividerDrawerItem(),
						drawerManager.getDrawerItemShuffle(this, DbHelper.isShuffle(), INDEX_SHUFFLE),
						drawerManager.getDrawerItemBuffer(this, INDEX_BUFFER),
						drawerManager.getDrawerItemReset(this, INDEX_RESET)
				);
		if(savedInstanceState!=null) drawerBuilder.withSavedInstance(savedInstanceState);
		else {
			Timber.e("savedInstanceState is null , getSelectedPlaylistIdFromPrefs=%d",
					DbHelper.getPlaylistId(getString(R.string.title_section_di)));
			drawerBuilder.withSelectedItem(DbHelper.getPlaylistId(getString(R.string.title_section_di)));
		}
		drawerBuilder.withHasStableIds(true);
		*//*if (isTablet()) {
			Drawer drawer = drawerBuilder.buildView();
			((ViewGroup) findViewById(R.id.nav_tablet)).addView(drawer.getSlider());
			return drawer;
		} *//*return drawerBuilder.build();
	}
	public boolean updateSleepDrawerItem(boolean isSleepActive){
		if(drawer!=null && drawer.getDrawerItem(INDEX_SLEEP)==null) {
			drawer.addItemAtPosition(
					drawerManager.getDrawerItemSleep(this,
							isSleepActive, INDEX_SLEEP),INDEX_SLEEP);
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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(drawer!=null) outState = drawer.saveInstanceState(outState);
		if(fastItemAdapter!=null) outState = fastItemAdapter.saveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().contains(INTENT_OPEN_APP))
			updateControlsDefault();

		else if (intent.getAction().contains(INTENT_CLOSE_APP)) {
			this.unbindService(mServiceConnection);
			finish();
		}
		super.onNewIntent(intent);
	}

	public SharedPreferences getPrefs(){
		return getSharedPreferences(RADIO_PREFERENCES, MODE_PRIVATE);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void loadingListStarted(){
		if(EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().unregister(this);
		}
		*//*if(loadingLayout==null) loadingLayout =(LinearLayout) findViewById(R.id.select_pls);
		closeDrawer();
		loadingLayout.setVisibility(View.VISIBLE);*//*
	}
	public void loadingFinished(){
		if(!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
		}
		*//*if(loadingLayout==null) loadingLayout =(LinearLayout) findViewById(R.id.select_pls);
		loadingLayout.setVisibility(View.GONE);*//*
		if(drawer!=null) setTitle(DbHelper.getPlaylistName(drawer.getCurrentSelection()));

		closeDrawer();
		updateControlsDefault();
		if(!(isServiceConnected() && serviceRadioRx.isSleepTimerRunning()))
			updateSleepDrawerItem(false);
	}
public void closeDrawer(){
	if(drawer!=null && drawer.isDrawerOpen()) drawer.closeDrawer();
	*//*if(getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);*//*
}
	public void openDrawer(){
		if(drawer!=null && !drawer.isDrawerOpen()) drawer.openDrawer();
		*//*if(getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);*//*
	}

	public void btnControlClick(String extra){
		Timber.v("btnControlClick: %s",extra);
		assertTrue(isServiceConnected());
		String urlToPlay=null;
		if(extra.contains(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED)){
			Intent intent = new Intent(getApplicationContext(), ServiceRadioRx.class);
			intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
			intent.putExtra(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED);
			startService(intent);
			return;
		}else if(extra.contains(ServiceRadioRx.EXTRA_PREV_PRESSED)){
			onPlayNextPrev(-1);
		}else if(extra.contains(ServiceRadioRx.EXTRA_NEXT_PRESSED)){
			onPlayNextPrev(1);
		}
	}
	public void btnControlClick(){
		Timber.v("btnControlClick playpause");
		assertTrue(isServiceConnected());
		//if(recyclerView!=null && getSelectedItem()!=null && getItemPosition(getSelectedItem())>=0) recyclerView.smoothScrollToPosition(getItemPosition(getSelectedItem()));
		Intent intent = new Intent(getApplicationContext(), ServiceRadioRx.class);
		intent.setAction(ServiceRadioRx.INTENT_USER_ACTION);
		intent.putExtra(ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED, ServiceRadioRx.EXTRA_PLAY_PAUSE_PRESSED);
		startService(intent);
	}
	public FastItemAdapter.OnClickListener<StationListItem> listItemOnClickListener=new FastAdapter.OnClickListener<StationListItem>() {
	@Override
	public boolean onClick(View v, IAdapter<StationListItem> adapter, StationListItem item, int position) {
		Timber.v("item %s",item.station.name);
		//adapter.getFastAdapter().select(position, false);
		//if(recyclerView!=null && getSelectedItem()!=null && getItemPosition(getSelectedItem())>=0) recyclerView.smoothScrollToPosition(getItemPosition(getSelectedItem()));
		bus.post(item);
		return true;
	}
};


	public Drawer.OnDrawerItemClickListener drawerItemClickListener= (view, position, drawerItem) -> {
		if(drawerItem.getIdentifier()<0) return false;
		showToast("drawerItem onClick "+ drawerItem.getIdentifier());
		Timber.v("drawerItem %d onClick", drawerItem.getIdentifier());
		SharedPreferences prefs = getPrefs();
		prefs.edit().putLong(SELECTED_PLAYLIST_POSITION, drawer.getCurrentSelection()).apply();
		loadingListStarted();
		setTitle(DbHelper.getPlaylistName(drawer.getCurrentSelection()));
		assertTrue(initFastAdapter(null, drawerItem.getIdentifier()));

		loadingFinished();
		return true;
	};
	public void initDrawerAndUI() {
		progressBar=(ProgressBar) findViewById(R.id.progressBar);
		dialogShower =new DialogShower();
		*//*btnNext = (ImageButton) findViewById(R.id.btnNext);
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
		infoProgress = (TextView) infoView.findViewById(R.id.textViewProgress);*//*
		mControlsFragment = (PlaybackControlsFragment) getFragmentManager()
				.findFragmentById(R.id.fragment_playback_controls);
		if (mControlsFragment == null) {
			throw new IllegalStateException("Mising fragment with id 'controls'. Cannot continue.");
		}

		showPlaybackControls();
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBufferUpdate(BufferUpdate bufferUpdate) {
		//Timber.e("progress %d / %d isPlaying=%b", bufferUpdate.getAudioBufferSizeMs(),bufferUpdate.getAudioBufferCapacityMs(),bufferUpdate.isPlaying());
		String p=bufferUpdate.getAudioBufferSizeMs()+" / "+ bufferUpdate.getAudioBufferCapacityMs();
		if(bufferUpdate.isPlaying()) p+=" isPlaying";
		if(infoProgress!=null) infoProgress.setText(p);
		progressBar.setProgress(bufferUpdate.getAudioBufferSizeMs() * progressBar.getMax() / bufferUpdate.getAudioBufferCapacityMs());
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMetadataUpdate(Metadata metadata) {
		mControlsFragment.onMetadataChanged(metadata);
	}
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onUiEvent(UiEvent eventUi) {
		UiEvent.UI_ACTION action = eventUi.getUiAction();
		String url = eventUi.getExtras().url;
		if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STARTED)) {
			Timber.v("PLAYBACK_STARTED");
			selectItem(fastItemAdapter.getAdapterItem(getItemPosition(url)));
			mControlsFragment.onPlaybackStateChanged(ActivityRxTest.UI_STATE.PLAYING, getSelectedItem().station.name, getSelectedItem().station.url);
		}
		else if (action.equals(UiEvent.UI_ACTION.PLAYBACK_STOPPED)) {
			Timber.v("PLAYBACK_STOPPED");
			selectItem(fastItemAdapter.getAdapterItem(getItemPosition(url)));
			mControlsFragment.onPlaybackStateChanged(ActivityRxTest.UI_STATE.IDLE, getSelectedItem().station.name, getSelectedItem().station.url);
		}
		else if (action.equals(UiEvent.UI_ACTION.LOADING_STARTED)) {
			Timber.v("LOADING_STARTED");
			selectItem(fastItemAdapter.getAdapterItem(getItemPosition(url)));
			mControlsFragment.onPlaybackStateChanged(UI_STATE.LOADING, getString(R.string.loading), getSelectedItem().station.url);
		}
		else if (action.equals(UiEvent.UI_ACTION.STATION_SELECTED)) {
				Timber.v("STATION_SELECTED");
			selectItem(fastItemAdapter.getAdapterItem(getItemPosition(url)));
			mControlsFragment.onPlaybackStateChanged(UI_STATE.LOADING, getSelectedItem().station.name, getSelectedItem().station.url);
		}
	}
	void updateControlsDefault(){
		int state= UI_STATE.IDLE;
		if(isServiceConnected()){
			state = (isPlaying() ? UI_STATE.PLAYING : UI_STATE.IDLE);
		}
		if(getSelectedItem()!=null) {
			mControlsFragment.onPlaybackStateChanged(state, getSelectedItem().station.name, getSelectedItem().station.url);
		}else {
			mControlsFragment.onPlaybackStateChanged(state, null, null);
		}
	}
	protected void showPlaybackControls() {
		Timber.d("showPlaybackControls");
		if (NetworkHelper.isOnline(this)) {
			getFragmentManager().beginTransaction()
					.setCustomAnimations(
							R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
							R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
					.show(mControlsFragment)
					.commit();
		}
	}



	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onSleepTimerStatusUpdate(SleepEvent sleepEvent) {
		SleepEvent.SLEEP_ACTION action=sleepEvent.getSleepAction();
		int seconds=sleepEvent.getSeconds();
		Log.d(TAG, "onSleepTimerStatusUpdate " + action + " " + seconds);
		if (action== SleepEvent.SLEEP_ACTION.CANCEL) {
			Toast.makeText(getApplicationContext(), "Sleep time cancelled", Toast.LENGTH_SHORT).show();
			drawer.updateItem(drawerManager.getDrawerItemSleep(this,false, INDEX_SLEEP));
		} else if (action== SleepEvent.SLEEP_ACTION.UPDATE) {
			if (seconds > 1) {
				if(seconds>60) showToast(seconds/60 + " minutes left");
				drawer.updateItem(
						drawerManager.getDrawerItemSleep(this,true, INDEX_SLEEP));

			}
		} else if (action== SleepEvent.SLEEP_ACTION.FINISH) {
			Toast.makeText(getApplicationContext(), "Sleep timer finished", Toast.LENGTH_SHORT).show();
			drawer.updateItem(
					drawerManager.getDrawerItemSleep(this,false, INDEX_SLEEP));
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


	public int selectNextItem() {

		int lastSelectedPosition = 0;
		if(getSelectedItem()!=null) lastSelectedPosition=getItemPosition(getSelectedItem());
		int newSelectedPosition = -1;
		if(DbHelper.isShuffle())
			newSelectedPosition=new Random(fastItemAdapter.getAdapterItemCount()-1).nextInt();
		else if (lastSelectedPosition < fastItemAdapter.getAdapterItemCount() - 2)
			newSelectedPosition=lastSelectedPosition + 1;
		else
			newSelectedPosition=0;
		assertTrue(newSelectedPosition>=0);
		bus.post(fastItemAdapter.getItem(newSelectedPosition));
		return newSelectedPosition;
	}

	public int selectPrevItem() {
		int lastSelectedPosition = 0;
		if(getSelectedItem()!=null) lastSelectedPosition=getItemPosition(getSelectedItem());
		int newSelectedPosition = -1;
		if (lastSelectedPosition > 0)
			newSelectedPosition=lastSelectedPosition - 1;
		else
			newSelectedPosition=0;
		assertTrue(newSelectedPosition>=0);

		bus.post(fastItemAdapter.getItem(newSelectedPosition));
		return newSelectedPosition;
	}

	public int  selectItem(StationListItem itemToSelect) {
		int newSelectedPosition = getItemPosition(itemToSelect);
		int oldSelectedPosition = -1;
		if(getSelectedItem()!=null)  oldSelectedPosition=getItemPosition(getSelectedItem());
		if(newSelectedPosition>=0) {
			if(oldSelectedPosition!=newSelectedPosition)fastItemAdapter.select(newSelectedPosition, false);
			if (recyclerView != null) recyclerView.smoothScrollToPosition(newSelectedPosition);
			Timber.v("selected position %d", newSelectedPosition);
		}else Timber.e("selectItem ERROR %s", itemToSelect.station.name);
		return newSelectedPosition;
	}
	@Subscribe()
	public void onPlayNextPrev(int value){
		if(value>0){
			Timber.v("onPlayNext");
			selectNextItem();
		}else {
			Timber.v("onPlayPrev");
			selectPrevItem();
		}
	}*/
}
