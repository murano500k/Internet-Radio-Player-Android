package com.stc.radio.player.ui;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DbHelper;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;


public class DrawerManager {


	public static final String TAG="DrawerManager";
	private Toast toast;


	public PrimaryDrawerItem getPlaylistDrawerItem(
			Activity activityRxTest, Drawer.OnDrawerItemClickListener listener, int title, long i){
		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withName(title)
				.withIcon(activityRxTest.getResources().getDrawable(R.drawable.ic_checked))
				.withTextColorRes(R.color.colorText)
				.withSelectedColorRes(R.color.colorAccent)
				.withOnDrawerItemClickListener(listener);
	}

	SwitchDrawerItem getDrawerItemShuffle(Activity activity, boolean isShuffle, int i) {
		return new SwitchDrawerItem()
				.withName(R.string.title_section_shuffle)
				.withTextColorRes(R.color.colorTextAccent)
				.withSelectable(false)
				.withIdentifier(i)
				.withLevel(2)
				.withChecked(isShuffle)
				.withOnCheckedChangeListener((drawerItem, buttonView, isChecked) -> {
					Timber.v("action_shuffle %b->%b", isChecked,DbHelper.isShuffle());
					DbHelper.setShuffle(isChecked);
					showToast(activity, isChecked ? "Shuffle enabled" : "Shuffle disabled");
				});
	}

	SwitchDrawerItem getDrawerItemSleep(Activity activityRxTest, boolean sleepRunning, int i) {
		return new SwitchDrawerItem()
				.withIdentifier(i)
				.withName(R.string.title_section_sleep)
				.withTextColorRes(R.color.colorTextAccent)
				.withSelectable(false)
				.withLevel(2)
				.withChecked(sleepRunning)
				.withOnCheckedChangeListener((drawerItem, buttonView, isChecked) -> {

					/*if(!activityRxTest.isServiceConnected()) {
						showToast(activityRxTest, "service not connected");

						return;
					}
					Log.v(TAG, "action_sleeptimer. was clicked: " +activityRxTest.getService().isSleepTimerRunning());
					activityRxTest.runOnUiThread(() -> {
						if(activityRxTest.getService().isSleepTimerRunning())
							activityRxTest.getDialogShower().showCancelTimerDialog(activityRxTest);
						else activityRxTest.getDialogShower().showSetTimerDialog(activityRxTest);
					});*/
				});
	}

	public PrimaryDrawerItem getDrawerItemReset(Activity activity,int i ) {

		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withSelectable(false)
				.withLevel(2)
				.withName(R.string.title_section_reset)
				.withTextColorRes(R.color.colorTextAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
					Log.v(TAG, "action_reset. was clicked");
					/*activityRxTest.drawer.closeDrawer();

					if(activityRxTest.isServiceConnected()) {
						activityRxTest.showToast("service not connected");
						return false;
					}
					showToast(activityRxTest, "Player reset");
					Intent intentReset=new Intent(activityRxTest,ServiceRadioRx.class);
					intentReset.setAction(ServiceRadioRx.INTENT_RESET);
					activityRxTest.startService(intentReset);*/
					return true;
				});
	}

	PrimaryDrawerItem getDrawerItemBuffer(Activity activity,int i ) {

		return new PrimaryDrawerItem()
				.withIdentifier(i)
				.withSelectable(false)
				.withLevel(2)
				.withName(R.string.title_section_buffer)
				.withTextColorRes(R.color.colorTextAccent)
				.withOnDrawerItemClickListener((view, position, drawerItem) -> {
//					Log.v(TAG, "action_buffer");
//					if(!activityRxTest.isServiceConnected()) {
//						activityRxTest.showToast("service not connected");
//						return false;
//					}
//					activityRxTest.runOnUiThread(() -> activityRxTest.getDialogShower().showDialogSetBufferSize(activityRxTest,activityRxTest.getLayoutInflater()));
					return true;
				});
	}
	public void showToast(Context context,String text){
		Observable.just(1)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(integer -> {
					if(toast!=null){
						toast.cancel();
						toast=null;
					}
					toast= Toast.makeText(context, text,Toast.LENGTH_SHORT);
					toast.show();
				});
	}


}
