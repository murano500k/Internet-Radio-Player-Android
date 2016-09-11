package com.android.murano500k.newradio.ui;

import android.content.Intent;
import android.util.Log;

import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;
import com.android.murano500k.newradio.UrlManager;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;

import static com.android.murano500k.newradio.UrlManager.CR;
import static com.android.murano500k.newradio.UrlManager.DI;
import static com.android.murano500k.newradio.UrlManager.INDEX_CR;
import static com.android.murano500k.newradio.UrlManager.INDEX_CUSTOM;
import static com.android.murano500k.newradio.UrlManager.INDEX_DI;
import static com.android.murano500k.newradio.UrlManager.INDEX_JR;
import static com.android.murano500k.newradio.UrlManager.INDEX_RR;
import static com.android.murano500k.newradio.UrlManager.INDEX_RT;
import static com.android.murano500k.newradio.UrlManager.JR;
import static com.android.murano500k.newradio.UrlManager.RR;
import static com.android.murano500k.newradio.UrlManager.RT;


public class DrawerManager {
	private final ActivityRxTest activity;
	private Drawer drawer;
	private static final int INDEX_SHUFFLE=7;
	static final int INDEX_SLEEP=8;
	private static final int INDEX_RESET=9;
	private static final int INDEX_BUFFER=10;


	public static final String TAG="DrawerManager";

	private ActivityRxTest getActivity() {
		return activity;
	}
	Drawer getDrawer() {
		return drawer;
	}

	DrawerManager(ActivityRxTest activityRxTest) {
		this.activity = activityRxTest;
	}

	boolean initDrawer(boolean isShuffle, boolean isSleep, int selectedIndex) {
		Log.d(TAG, "initDrawer, selected "+ selectedIndex );
		Log.d(TAG, "initDrawer, drawer is null "+ (drawer == null)  );

		if (drawer != null) drawer.resetDrawerContent();
			drawer = new DrawerBuilder()
					.withActivity(getActivity())
					.withActionBarDrawerToggle(true)
					.withTranslucentNavigationBar(true)
					.withTranslucentNavigationBarProgrammatically(true)
					.withTranslucentStatusBar(true)
					.withSliderBackgroundColorRes(R.color.colorSecondary)
					.addDrawerItems(
							new SectionDrawerItem().withName(R.string.title_section_stations).withTextColorRes(R.color.colorText).withIdentifier(99),
							getPlaylistDrawerItem(R.string.title_section_di,
									UrlManager.getPlaylistUrl(DI), INDEX_DI),
							getPlaylistDrawerItem(R.string.title_section_radiotunes,
									UrlManager.getPlaylistUrl(RT), INDEX_RT),
							getPlaylistDrawerItem(R.string.title_section_jazz,
									UrlManager.getPlaylistUrl(JR), INDEX_JR),
							getPlaylistDrawerItem(R.string.title_section_rock,
									UrlManager.getPlaylistUrl(RR), INDEX_RR),
							getPlaylistDrawerItem(R.string.title_section_classic,
									UrlManager.getPlaylistUrl(CR), INDEX_CR),
							getCustomStationsDrawerItem(INDEX_CUSTOM),
							new DividerDrawerItem(),
							//new SectionDrawerItem().withName(R.string.title_section_settings).withTextColorRes(R.color.colorText).withIdentifier(98),
							getDrawerItemShuffle(isShuffle, INDEX_SHUFFLE),
							getDrawerItemSleep(isSleep, INDEX_SLEEP),
							new DividerDrawerItem(),
							getDrawerItemBuffer(INDEX_BUFFER),
							getDrawerItemReset(INDEX_RESET)
					)
					.withSelectedItem(selectedIndex)
					.withHasStableIds(true)
					.build();
			return true;
	}

	private PrimaryDrawerItem getCustomStationsDrawerItem(int i) {
		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withName(R.string.title_section_custom)
				.withTextColorRes(R.color.colorText)
				.withSelectedColorRes(R.color.colorAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
					Log.v(TAG, "getPlaylistDrawerItem. was clicked. "+ drawerItem.getIdentifier());
					drawer.closeDrawer();
					if(getActivity().getService()==null || !getActivity().getService().isServiceConnected) {
						getActivity().showToast("service not connected");
						return false;
					}
					getActivity().runOnUiThread(() -> {
						Log.v(TAG, "action_select_pls");
						getActivity().selectPlsIntent();
					});
					return true;
				});
	}

	private PrimaryDrawerItem getPlaylistDrawerItem(int title, String urlStr, int i){
		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withName(title)
				.withTextColorRes(R.color.colorText)
				.withSelectedColorRes(R.color.colorAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
					Log.v(TAG, "getPlaylistDrawerItem. was clicked. "+ drawerItem.getIdentifier());
					drawer.closeDrawer();
					getActivity().showToast("Loading playlist...");
					Log.v(TAG, "getPlaylistDrawerItem. was clicked. "+ drawerItem.getIdentifier());
					getActivity().getPlaylistDownloader().downloadPlaylist(urlStr, (int) drawerItem.getIdentifier());
					return true;
				});
	}

	private SwitchDrawerItem getDrawerItemShuffle(boolean isShuffle, int i) {
		return new SwitchDrawerItem()
				.withName(R.string.title_section_shuffle)
				.withTextColorRes(R.color.colorTextAccent)
				.withSelectable(false)
				.withIdentifier(i)
				.withLevel(3)
				.withChecked(isShuffle)
				.withOnCheckedChangeListener((drawerItem, buttonView, isChecked) -> {
					Log.v(TAG, "action_shuffle");
					getActivity().getPlaylistManager().setShuffle(isChecked);
					getActivity().showToast(getActivity().getPlaylistManager().isShuffle() ? "Shuffle enabled" : "Shuffle disabled");
				});
	}

	SwitchDrawerItem getDrawerItemSleep(boolean sleepRunning, int i) {
		return new SwitchDrawerItem()
				.withIdentifier(i)
				.withName(R.string.title_section_sleep)
				.withTextColorRes(R.color.colorTextAccent)
				.withSelectable(false)
				.withLevel(3)
				.withChecked(sleepRunning)
				.withOnCheckedChangeListener((drawerItem, buttonView, isChecked) -> {
					Log.v(TAG, "action_sleeptimer. was clicked: " +getActivity().getService().isSleepTimerRunning());
					//drawer.closeDrawer();

					if(getActivity().getService()==null || !getActivity().getService().isServiceConnected) {
						getActivity().showToast("service not connected");
					}
					getActivity().runOnUiThread(() -> {
						if(getActivity().getService().isSleepTimerRunning())
							getActivity().getDialogShower().showCancelTimerDialog(getActivity());
						else getActivity().getDialogShower().showSetTimerDialog(getActivity());
					});
				});
	}

	private PrimaryDrawerItem getDrawerItemReset(int i) {

		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withSelectable(false)
				.withLevel(2)
				.withName(R.string.title_section_reset)
				.withTextColorRes(R.color.colorTextAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
					Log.v(TAG, "action_reset. was clicked");
					drawer.closeDrawer();

					if(getActivity().getService()==null || !getActivity().getService().isServiceConnected) {
						getActivity().showToast("service not connected");
						return false;
					}
					getActivity().showToast("Player reset");
					Intent intentReset=new Intent(getActivity(),ServiceRadioRx.class);
					intentReset.setAction(ServiceRadioRx.INTENT_RESET);
					getActivity().startService(intentReset);
					return true;
				});
	}

	private PrimaryDrawerItem getDrawerItemBuffer(int i) {
		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withSelectable(false)
				.withLevel(2)
				.withName(R.string.title_section_buffer)
				.withTextColorRes(R.color.colorTextAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
					Log.v(TAG, "action_buffer");
					//drawer.closeDrawer();
					if(getActivity().getService()==null || !getActivity().getService().isServiceConnected) {
						getActivity().showToast("service not connected");
						return false;
					}
					getActivity().runOnUiThread(() -> getActivity().getDialogShower().showDialogSetBufferSize(getActivity(),getActivity().getLayoutInflater()));
					return true;
				});
	}

}
